import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import type { FullConfig } from "@playwright/test";
import { runtimeStatePath, type E2eRuntimeState } from "./runtime-state";

const API_URL = process.env.SHOPPEW_API_URL ?? "http://localhost:28080";
const DEFAULT_PASSWORD = "ShoppewSmoke2026!";

interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error?: { code?: string; message?: string };
}

interface AuthResponse {
  accessToken: string;
}

interface ProductAttribute {
  name: string;
  value: string;
  valueType: string;
  required: boolean;
}

interface ProductDetail {
  categoryId?: string;
  categoryName?: string;
  attributes?: ProductAttribute[];
}

interface CartItem {
  id?: string;
}

interface CartShop {
  items?: CartItem[];
}

interface Cart {
  shops?: CartShop[];
}

type ApiOptions = {
  method?: string;
  token?: string;
  json?: unknown;
  body?: BodyInit;
};

async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const headers = new Headers({ "X-Request-Id": `e2e-${Date.now()}` });
  if (options.token) headers.set("Authorization", `Bearer ${options.token}`);
  let body = options.body;
  if (options.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.json);
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body,
  });
  const text = await response.text();
  let envelope: ApiEnvelope<T> | undefined;
  try {
    envelope = JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    // The response details below retain enough evidence for a useful failure.
  }
  if (!response.ok || !envelope?.success) {
    throw new Error(
      `${options.method ?? "GET"} ${path} failed (${response.status}): ${envelope?.error?.code ?? "INVALID_RESPONSE"} ${envelope?.error?.message ?? text.slice(0, 500)}`,
    );
  }
  return envelope.data;
}

async function waitForBackend(): Promise<void> {
  const deadline = Date.now() + 120_000;
  let lastError: unknown;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${API_URL}/actuator/health/readiness`);
      if (response.ok && ((await response.json()) as { status?: string }).status === "UP") return;
    } catch (error) {
      lastError = error;
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 1_000));
  }
  throw new Error(`Backend was not ready at ${API_URL}: ${String(lastError ?? "readiness timeout")}`);
}

function seedRuntime(): string {
  const suppliedRunId = process.env.SHOPPEW_E2E_RUN_ID?.trim();
  if (suppliedRunId) {
    if (!/^\d{14}$/.test(suppliedRunId)) {
      throw new Error("SHOPPEW_E2E_RUN_ID must use yyyyMMddHHmmss format");
    }
    return suppliedRunId;
  }

  const shell = process.platform === "win32" ? "powershell.exe" : "pwsh";
  const script = resolve(process.cwd(), "scripts", "smoke-catalog.ps1");
  const output = execFileSync(
    shell,
    ["-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script, "-BaseUrl", API_URL],
    { cwd: process.cwd(), encoding: "utf8", timeout: 15 * 60_000, maxBuffer: 10 * 1024 * 1024 },
  );
  const runId = output.match(/^Run:\s*(\d{14})\s*$/m)?.[1];
  if (!output.includes("Status: PASS") || !runId) {
    throw new Error(`Catalog/commerce seed did not complete successfully:\n${output.slice(-4_000)}`);
  }
  return runId;
}

async function login(email: string, password: string, deviceName: string): Promise<string> {
  const auth = await api<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    json: { email, password, deviceName },
  });
  return auth.accessToken;
}

async function clearBuyerCart(token: string): Promise<void> {
  const cart = await api<Cart>("/api/v1/cart", { token });
  const itemIds = cart.shops?.flatMap((shop) => shop.items?.flatMap((item) => item.id ? [item.id] : []) ?? []) ?? [];
  for (const itemId of itemIds) {
    await api(`/api/v1/cart/items/${itemId}`, { method: "DELETE", token });
  }
}

export default async function globalSetup(_config: FullConfig): Promise<void> {
  await waitForBackend();
  const runId = seedRuntime();
  const password = process.env.SHOPPEW_E2E_PASSWORD ?? DEFAULT_PASSWORD;
  const buyerEmail = `catalog-buyer-${runId}@example.test`;
  const sellerEmail = `catalog-seller-${runId}@example.test`;
  const adminEmail = `catalog-admin-${runId}@example.test`;
  const productSlug = `shoppew-runtime-tee-${runId}`;
  const productName = `Shoppew Runtime Tee ${runId}`;

  const buyerToken = await login(buyerEmail, password, "shoppew Playwright setup buyer");
  await clearBuyerCart(buyerToken);
  const source = await api<ProductDetail>(`/api/v1/public/products/${encodeURIComponent(productSlug)}`);
  if (!source.categoryId || !source.categoryName) {
    throw new Error("The runtime product has no category identity for Seller UI product creation");
  }
  const uniqueSuffix = Date.now().toString(36);
  const state: E2eRuntimeState = {
    runId,
    password,
    buyerEmail,
    sellerEmail,
    adminEmail,
    shopName: `Smoke Market ${runId}`,
    productName,
    productSlug,
    pendingProductName: `Shoppew E2E Moderation ${runId}-${uniqueSuffix}`,
    pendingProductSlug: `shoppew-e2e-moderation-${runId}-${uniqueSuffix}`,
    sourceCategoryId: source.categoryId,
    sourceCategoryName: source.categoryName,
    sourceAttributes: (source.attributes ?? []).map((attribute) => ({
      name: attribute.name,
      value: attribute.value,
      valueType: attribute.valueType,
      required: attribute.required,
    })),
  };
  mkdirSync(resolve(process.cwd(), "test-results"), { recursive: true });
  writeFileSync(runtimeStatePath, JSON.stringify(state, null, 2), "utf8");
}
