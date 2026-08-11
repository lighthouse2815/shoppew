import { existsSync, readFileSync } from "node:fs";
import type { FullConfig } from "@playwright/test";
import { runtimeStatePath, type E2eRuntimeState } from "./runtime-state";

const API_URL = process.env.SHOPPEW_API_URL ?? "http://localhost:28080";

interface Envelope<T> {
  success: boolean;
  data: T;
}

interface AuthResponse {
  accessToken: string;
}

interface Shop {
  id: string;
}

interface Cart {
  shops?: Array<{ items?: Array<{ id?: string }> }>;
}

interface OrderDetail {
  status: string;
}

async function readEnvelope<T>(response: Response): Promise<T> {
  const envelope = (await response.json()) as Envelope<T>;
  if (!response.ok || !envelope.success) throw new Error(`HTTP ${response.status}`);
  return envelope.data;
}

export default async function globalTeardown(_config: FullConfig): Promise<void> {
  if (!existsSync(runtimeStatePath)) return;
  const state = JSON.parse(readFileSync(runtimeStatePath, "utf8")) as E2eRuntimeState;
  let buyerHeaders: Record<string, string> | undefined;
  let orderStatus: string | undefined;

  try {
    const buyerAuth = await readEnvelope<AuthResponse>(await fetch(`${API_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Request-Id": `e2e-cleanup-${Date.now()}` },
      body: JSON.stringify({
        email: state.buyerEmail,
        password: state.password,
        deviceName: "shoppew Playwright cart cleanup",
      }),
    }));
    buyerHeaders = { Authorization: `Bearer ${buyerAuth.accessToken}`, "X-Request-Id": `e2e-cleanup-${Date.now()}` };
    const cart = await readEnvelope<Cart>(await fetch(`${API_URL}/api/v1/cart`, { headers: buyerHeaders }));
    const itemIds = cart.shops?.flatMap((shop) => shop.items?.flatMap((item) => item.id ? [item.id] : []) ?? []) ?? [];
    for (const itemId of itemIds) {
      await readEnvelope(await fetch(`${API_URL}/api/v1/cart/items/${itemId}`, { method: "DELETE", headers: buyerHeaders }));
    }
    if (state.lifecycleOrderId) {
      const order = await readEnvelope<OrderDetail>(await fetch(
        `${API_URL}/api/v1/orders/${state.lifecycleOrderId}`,
        { headers: buyerHeaders },
      ));
      orderStatus = order.status;
      if (orderStatus === "PENDING_PAYMENT") {
        const cancelled = await readEnvelope<OrderDetail>(await fetch(
          `${API_URL}/api/v1/orders/${state.lifecycleOrderId}/cancel`,
          {
            method: "POST",
            headers: { ...buyerHeaders, "Content-Type": "application/json" },
            body: JSON.stringify({ reason: "Playwright teardown after an incomplete scenario" }),
          },
        ));
        orderStatus = cancelled.status;
      } else if (orderStatus === "DELIVERED") {
        const completed = await readEnvelope<OrderDetail>(await fetch(
          `${API_URL}/api/v1/orders/${state.lifecycleOrderId}/complete`,
          { method: "POST", headers: buyerHeaders },
        ));
        orderStatus = completed.status;
      }
    }
  } catch (error) {
    console.warn(`E2E buyer cleanup could not finish: ${String(error)}`);
  }

  const unfinishedSellerOrder = Boolean(
    state.lifecycleOrderId
    && !["COMPLETED", "CANCELLED", "PARTIALLY_REFUNDED", "REFUNDED"].includes(orderStatus ?? ""),
  );
  if (!state.pendingProductId && !unfinishedSellerOrder) return;
  try {
    const sellerAuth = await readEnvelope<AuthResponse>(await fetch(`${API_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Request-Id": `e2e-cleanup-${Date.now()}` },
      body: JSON.stringify({
        email: state.sellerEmail,
        password: state.password,
        deviceName: "shoppew Playwright product cleanup",
      }),
    }));
    const authorization = { Authorization: `Bearer ${sellerAuth.accessToken}`, "X-Request-Id": `e2e-cleanup-${Date.now()}` };
    const shops = await readEnvelope<Shop[]>(await fetch(`${API_URL}/api/v1/seller/shops`, { headers: authorization }));
    if (!shops[0]?.id) return;

    if (state.lifecycleOrderId && unfinishedSellerOrder) {
      const orderBase = `${API_URL}/api/v1/seller/shops/${shops[0].id}/orders/${state.lifecycleOrderId}`;
      const current = await readEnvelope<OrderDetail>(await fetch(orderBase, { headers: authorization }));
      orderStatus = current.status;
      if (["CONFIRMED", "PROCESSING"].includes(orderStatus)) {
        const cancelled = await readEnvelope<OrderDetail>(await fetch(`${orderBase}/cancel`, {
          method: "POST",
          headers: { ...authorization, "Content-Type": "application/json" },
          body: JSON.stringify({ reason: "Playwright teardown after an incomplete scenario" }),
        }));
        orderStatus = cancelled.status;
      } else {
        if (orderStatus === "READY_TO_SHIP") {
          const shipped = await readEnvelope<OrderDetail>(await fetch(`${orderBase}/ship`, {
            method: "POST",
            headers: { ...authorization, "Content-Type": "application/json" },
            body: JSON.stringify({
              trackingNumber: `E2E-CLEANUP-${state.runId}`,
              location: "Playwright cleanup hub",
            }),
          }));
          orderStatus = shipped.status;
        }
        if (orderStatus === "SHIPPED") {
          const delivered = await readEnvelope<OrderDetail>(await fetch(`${orderBase}/deliver`, {
            method: "POST",
            headers: { ...authorization, "Content-Type": "application/json" },
            body: JSON.stringify({ location: "Playwright cleanup recipient" }),
          }));
          orderStatus = delivered.status;
        }
        if (orderStatus === "DELIVERED" && buyerHeaders) {
          const completed = await readEnvelope<OrderDetail>(await fetch(
            `${API_URL}/api/v1/orders/${state.lifecycleOrderId}/complete`,
            { method: "POST", headers: buyerHeaders },
          ));
          orderStatus = completed.status;
        }
      }
    }

    if (state.pendingProductId) {
      const response = await fetch(
        `${API_URL}/api/v1/seller/shops/${shops[0].id}/products/${state.pendingProductId}`,
        { method: "DELETE", headers: authorization },
      );
      await readEnvelope(response);
    }
  } catch (error) {
    // Cleanup must not replace the original browser failure, but it remains
    // visible in terminal output so local catalog pollution is diagnosable.
    console.warn(`E2E seller cleanup could not finish for order ${state.lifecycleOrderId ?? "n/a"} and product ${state.pendingProductId ?? "n/a"}: ${String(error)}`);
  }
}
