import { expect, test, type Page, type Response } from "@playwright/test";
import { readRuntimeState, writeRuntimeState, type E2eRuntimeState } from "./runtime-state";

const API_URL = process.env.SHOPPEW_API_URL ?? "http://localhost:28080";
const STOREFRONT_URL = process.env.SHOPPEW_STOREFRONT_URL ?? "http://localhost:3000";
const SELLER_URL = process.env.SHOPPEW_SELLER_URL ?? "http://localhost:3001";
const ADMIN_URL = process.env.SHOPPEW_ADMIN_URL ?? "http://localhost:3002";
const PNG_BYTES = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
  "base64",
);

interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error?: { code?: string; message?: string };
}

interface CheckoutPreviewData {
  itemsSubtotal: number;
  shippingTotal: number;
  discountTotal: number;
  grandTotal: number;
  currency: string;
  shops?: Array<{ shopName?: string; cartItemIds?: string[] }>;
}

interface CheckoutData {
  checkoutNumber: string;
  status: string;
  grandTotal: number;
  currency: string;
  orders?: Array<{ id?: string; orderNumber?: string; status?: string }>;
  payment?: { provider?: string; status?: string };
}

interface ProductData {
  id: string;
  status: string;
}

interface VariantData {
  id: string;
}

interface InventoryData {
  availableQuantity: number;
}

interface OrderDetailData {
  status: string;
  customerNote?: string;
  items?: Array<{
    productName?: string;
    sku?: string;
    quantity?: number;
    unitPrice?: number;
  }>;
  history?: Array<{ toStatus?: string }>;
  shipment?: { trackingNumber?: string; status?: string };
}

function isApiCall(response: Response, path: string, method: string): boolean {
  return response.url().startsWith(API_URL)
    && new URL(response.url()).pathname === path
    && response.request().method() === method;
}

async function responseData<T>(response: Response, expectedStatus: number | number[] = 200): Promise<T> {
  const expected = Array.isArray(expectedStatus) ? expectedStatus : [expectedStatus];
  const envelope = await response.json() as ApiEnvelope<T>;
  expect(expected, `${response.request().method()} ${response.url()}`).toContain(response.status());
  expect(envelope.success, envelope.error?.message ?? envelope.error?.code ?? response.url()).toBe(true);
  return envelope.data;
}

async function login(page: Page, appUrl: string, email: string, password: string): Promise<void> {
  await page.goto(`${appUrl}/login`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Mật khẩu").fill(password);
  const responsePromise = page.waitForResponse(
    (response) => isApiCall(response, "/api/v1/auth/login", "POST"),
  );
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  await responseData(await responsePromise);
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
}

function requiredLifecycleOrder(state: E2eRuntimeState): { id: string; number: string } {
  if (!state.lifecycleOrderId || !state.lifecycleOrderNumber) {
    throw new Error("The buyer UI test did not persist its lifecycle order");
  }
  return { id: state.lifecycleOrderId, number: state.lifecycleOrderNumber };
}

async function createModerationProductThroughSellerUi(page: Page, state: E2eRuntimeState): Promise<void> {
  await page.goto(`${SELLER_URL}/products/new`);
  await expect(page.getByRole("heading", { name: "Tạo sản phẩm", exact: true })).toBeVisible();
  await page.getByLabel("Tên sản phẩm").fill(state.pendingProductName);
  await page.getByLabel("Slug (không bắt buộc)").fill(state.pendingProductSlug);

  const category = page.getByLabel("Danh mục *");
  await expect(category.locator(`option[value="${state.sourceCategoryId}"]`)).toContainText(state.sourceCategoryName);
  await category.selectOption(state.sourceCategoryId);
  await page.getByLabel("Mô tả ngắn").fill("Listing được tạo hoàn toàn qua Seller Center trong Playwright E2E.");
  await page.getByLabel("Mô tả chi tiết *").fill(
    "Sản phẩm tạm dùng để kiểm tra cấu hình, tồn kho, gửi duyệt và kiểm duyệt trên backend thật.",
  );

  const createResponsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.url().startsWith(API_URL)
      && /^\/api\/v1\/seller\/shops\/[^/]+\/products$/.test(url.pathname)
      && response.request().method() === "POST";
  });
  await page.getByRole("button", { name: "Tạo bản nháp" }).click();
  const draft = await responseData<ProductData>(await createResponsePromise, 201);
  expect(draft.status).toBe("DRAFT");
  state.pendingProductId = draft.id;
  writeRuntimeState(state);
  await expect(page).toHaveURL(`${SELLER_URL}/products/${draft.id}`);
  await expect(page.getByRole("heading", { name: state.pendingProductName, exact: true })).toBeVisible();

  const suppliedAttributes = state.sourceAttributes.filter((attribute) => attribute.required);
  for (const attribute of suppliedAttributes) {
    const field = page.getByLabel(`${attribute.name} *`, { exact: true });
    if (attribute.valueType === "BOOLEAN") {
      await field.selectOption(attribute.value);
    } else {
      await field.fill(attribute.value);
    }
  }
  if (suppliedAttributes.length > 0) {
    const attributesResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, `/api/v1/seller/shops/${new URL(response.url()).pathname.split("/")[5]}/products/${draft.id}/attributes`, "PUT"),
    );
    await page.getByRole("button", { name: "Lưu thuộc tính" }).click();
    await responseData(await attributesResponsePromise);
    await expect(page.getByRole("status")).toContainText("Đã lưu thuộc tính sản phẩm.");
  }

  await page.locator(".upload-tile input[type=file]").setInputFiles({
    name: `shoppew-e2e-${state.runId}.png`,
    mimeType: "image/png",
    buffer: PNG_BYTES,
  });
  await page.getByLabel("Mô tả ảnh").fill(state.pendingProductName);
  await page.getByLabel("Đặt làm ảnh chính").check();
  const imageResponsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.url().startsWith(API_URL)
      && url.pathname.endsWith(`/products/${draft.id}/images`)
      && response.request().method() === "POST";
  });
  await page.getByRole("button", { name: "Tải ảnh lên" }).click();
  await responseData(await imageResponsePromise, 201);
  await expect(page.getByText("Ảnh chính", { exact: true })).toBeVisible();

  const sku = `E2E-MOD-${state.runId}-${draft.id.slice(0, 8)}`;
  await page.getByLabel("SKU", { exact: true }).fill(sku);
  await page.getByLabel("Tên biến thể", { exact: true }).fill("Mặc định E2E");
  await page.getByLabel("Giá bán (VND)", { exact: true }).fill("199000");
  await page.getByLabel("Khối lượng (g)", { exact: true }).fill("200");
  const variantResponsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.url().startsWith(API_URL)
      && url.pathname.endsWith(`/products/${draft.id}/variants`)
      && response.request().method() === "POST";
  });
  await page.getByRole("button", { name: "Thêm biến thể", exact: true }).click();
  const variant = await responseData<VariantData>(await variantResponsePromise, 201);
  await expect(page.getByText(sku, { exact: false })).toBeVisible();

  await page.goto(`${SELLER_URL}/inventory`);
  await page.getByLabel("Tìm theo sản phẩm hoặc SKU").fill(sku);
  const inventorySearchPromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.url().startsWith(API_URL)
      && url.pathname.endsWith("/inventory")
      && url.searchParams.get("q") === sku
      && response.request().method() === "GET";
  });
  await page.getByRole("button", { name: "Tìm", exact: true }).click();
  await responseData(await inventorySearchPromise);
  const inventoryRow = page.getByRole("row").filter({ hasText: sku });
  await expect(inventoryRow).toContainText(state.pendingProductName);
  await inventoryRow.getByRole("button", { name: "Điều chỉnh" }).click();
  await page.getByLabel("Kiểu điều chỉnh").selectOption("INCREASE");
  await page.getByLabel("Số lượng", { exact: true }).fill("8");
  await page.getByLabel("Ngưỡng cảnh báo").fill("2");
  await page.getByLabel("Ghi chú").fill(`Playwright E2E stock ${state.runId}`);
  const inventoryResponsePromise = page.waitForResponse((response) =>
    isApiCall(response, `/api/v1/seller/shops/${new URL(response.url()).pathname.split("/")[5]}/inventory/${variant.id}/adjustments`, "POST"),
  );
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  const inventory = await responseData<InventoryData>(await inventoryResponsePromise);
  expect(inventory.availableQuantity).toBe(8);
  await expect(inventoryRow).toContainText("8");

  await page.goto(`${SELLER_URL}/products/${draft.id}`);
  await expect(page.getByRole("heading", { name: state.pendingProductName, exact: true })).toBeVisible();
  const submitResponsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.url().startsWith(API_URL)
      && url.pathname.endsWith(`/products/${draft.id}/submit`)
      && response.request().method() === "POST";
  });
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  const submitted = await responseData<ProductData>(await submitResponsePromise);
  expect(submitted.status).toBe("PENDING_REVIEW");
  await expect(page.getByText("PENDING REVIEW", { exact: true })).toBeVisible();
}

test.describe.serial("shoppew real-backend critical marketplace lifecycle", () => {
  test.setTimeout(180_000);
  let state: E2eRuntimeState;

  test.beforeAll(() => {
    state = readRuntimeState();
  });

  test("buyer browses, adds to cart, accepts the authoritative COD quote and places a new order", async ({ page, request }) => {
    const readiness = await request.get(`${API_URL}/actuator/health/readiness`);
    expect(readiness.ok()).toBeTruthy();
    await expect(readiness.json()).resolves.toMatchObject({ status: "UP" });

    await page.goto(STOREFRONT_URL);
    await expect(page.getByRole("heading", { name: "Mua đúng thứ bạn cần, từ nhà bán bạn tin." })).toBeVisible();
    await login(page, STOREFRONT_URL, state.buyerEmail, state.password);

    await page.goto(`${STOREFRONT_URL}/search?q=${encodeURIComponent(state.productName)}`);
    await expect(page.getByRole("heading", { name: new RegExp(state.productName) })).toBeVisible();
    const productLink = page.getByRole("link", { name: state.productName }).first();
    await expect(productLink).toHaveAttribute("href", `/product/${state.productSlug}`);
    await productLink.click();
    await expect(page).toHaveURL(`${STOREFRONT_URL}/product/${state.productSlug}`);
    await expect(page.getByRole("heading", { name: state.productName, exact: true })).toBeVisible();

    const addCartResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, "/api/v1/cart/items", "POST"),
    );
    await page.getByRole("button", { name: /Thêm vào giỏ/ }).click();
    await responseData(await addCartResponsePromise, 201);
    await expect(page.getByText("Đã thêm sản phẩm vào giỏ hàng.")).toBeVisible();

    await page.getByLabel("Giỏ hàng", { exact: true }).click();
    await expect(page.getByRole("heading", { name: "Giỏ hàng", exact: true })).toBeVisible();
    await expect(page.getByText(state.productName)).toBeVisible();
    await expect(page.getByText("Đã chọn").locator(".."), "The isolated cart should contain one selected item").toContainText("1 sản phẩm");

    const previewResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, "/api/v1/checkout/preview", "POST"),
    );
    await page.getByRole("link", { name: "Tiến hành thanh toán" }).click();
    await responseData<CheckoutPreviewData>(await previewResponsePromise);
    await expect(page.getByRole("heading", { name: "Thanh toán", exact: true })).toBeVisible();
    await expect(page.getByText("Thanh toán khi nhận hàng", { exact: true })).toBeVisible();

    const customerNote = `Playwright lifecycle ${state.runId}`;
    const finalPreviewResponsePromise = page.waitForResponse((response) => {
      if (!isApiCall(response, "/api/v1/checkout/preview", "POST")) return false;
      try {
        return (response.request().postDataJSON() as { customerNote?: string }).customerNote === customerNote;
      } catch {
        return false;
      }
    });
    await page.getByLabel("Ghi chú cho nhà bán").fill(customerNote);
    const preview = await responseData<CheckoutPreviewData>(await finalPreviewResponsePromise);
    expect(preview.itemsSubtotal).toBeGreaterThan(0);
    expect(preview.shippingTotal).toBeGreaterThanOrEqual(0);
    expect(preview.grandTotal).toBe(preview.itemsSubtotal + preview.shippingTotal - preview.discountTotal);
    expect(preview.currency).toBe("VND");
    expect(preview.shops).toHaveLength(1);
    expect(preview.shops?.[0]?.cartItemIds).toHaveLength(1);
    await expect(page.getByRole("button", { name: "Đặt hàng", exact: true })).toBeEnabled();

    const checkoutResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, "/api/v1/checkout", "POST"),
    );
    await page.getByRole("button", { name: "Đặt hàng", exact: true }).click();
    const checkout = await responseData<CheckoutData>(await checkoutResponsePromise, 201);
    expect(checkout.status).toBe("CONFIRMED");
    expect(checkout.payment).toMatchObject({ provider: "COD", status: "PENDING" });
    expect(checkout.orders).toHaveLength(1);
    expect(checkout.orders?.[0]?.status).toBe("CONFIRMED");
    const order = checkout.orders?.[0];
    if (!order?.id || !order.orderNumber) throw new Error("Checkout did not return its seller order identity");
    state.lifecycleOrderId = order.id;
    state.lifecycleOrderNumber = order.orderNumber;
    state.lifecycleItemsSubtotal = preview.itemsSubtotal;
    state.lifecycleGrandTotal = checkout.grandTotal;
    writeRuntimeState(state);

    await expect(page).toHaveURL(/\/order\/success\?/);
    await expect(page.getByText("Đặt hàng thành công", { exact: true })).toBeVisible();
    await expect(page.getByText(checkout.checkoutNumber, { exact: true })).toBeVisible();
    await page.getByRole("link", { name: "Xem đơn hàng" }).click();
    await expect(page).toHaveURL(`${STOREFRONT_URL}/account/orders/${order.id}`);
    await expect(page.getByRole("heading", { name: `#${order.orderNumber}` })).toBeVisible();
    await expect(page.getByText(state.productName)).toBeVisible();
    await expect(page.getByText("Đã xác nhận", { exact: true }).first()).toBeVisible();
  });

  test("seller fulfills that order and creates a complete listing through Seller Center UI", async ({ page }) => {
    test.setTimeout(240_000);
    const order = requiredLifecycleOrder(state);
    await login(page, SELLER_URL, state.sellerEmail, state.password);
    await expect(page.getByRole("heading", { name: "Tổng quan vận hành" })).toBeVisible();
    await expect(page.getByRole("banner").getByText(state.shopName, { exact: true })).toBeVisible();

    await page.goto(`${SELLER_URL}/orders`);
    const orderRow = page.getByRole("row").filter({ hasText: order.number });
    await expect(orderRow).toBeVisible();
    await orderRow.getByRole("link", { name: "Xử lý" }).click();
    await expect(page.getByRole("heading", { name: new RegExp(order.number) })).toBeVisible();
    await expect(page.getByText(state.productName)).toBeVisible();

    async function transition(command: string, button: string, expectedStatus: string): Promise<OrderDetailData> {
      const responsePromise = page.waitForResponse((response) => {
        const url = new URL(response.url());
        return response.url().startsWith(API_URL)
          && url.pathname.endsWith(`/orders/${order.id}/${command}`)
          && response.request().method() === "POST";
      });
      await page.getByRole("button", { name: button, exact: true }).click();
      const detail = await responseData<OrderDetailData>(await responsePromise);
      expect(detail.status).toBe(expectedStatus);
      await expect(page.getByText(expectedStatus.replaceAll("_", " "), { exact: true }).first()).toBeVisible();
      return detail;
    }

    await page.getByLabel("Ghi chú nội bộ").fill(`Seller bắt đầu xử lý ${state.runId}`);
    await transition("process", "Bắt đầu xử lý", "PROCESSING");
    await page.getByLabel("Vị trí / điểm quét").fill("Kho shoppew E2E");
    await page.getByLabel("Ghi chú nội bộ").fill("Đóng gói hoàn tất");
    await transition("ready-to-ship", "Sẵn sàng giao", "READY_TO_SHIP");

    const trackingNumber = `E2E-${state.runId}`;
    await page.getByLabel("Mã vận đơn").fill(trackingNumber);
    await page.getByLabel("Vị trí / điểm quét").fill("Trung tâm shoppew E2E");
    const shipped = await transition("ship", "Bàn giao vận chuyển", "SHIPPED");
    expect(shipped.shipment).toMatchObject({ trackingNumber, status: "IN_TRANSIT" });
    await page.getByLabel("Vị trí / điểm quét").fill("Địa chỉ người nhận E2E");
    const delivered = await transition("deliver", "Xác nhận đã giao", "DELIVERED");
    expect(delivered.shipment).toMatchObject({ trackingNumber, status: "DELIVERED" });
    expect(delivered.history?.map((entry) => entry.toStatus)).toEqual([
      "CONFIRMED",
      "PROCESSING",
      "READY_TO_SHIP",
      "SHIPPED",
      "DELIVERED",
    ]);
    await expect(page.getByText("Không có bước xử lý seller tiếp theo ở trạng thái này.")).toBeVisible();
    state.lifecycleTrackingNumber = trackingNumber;
    writeRuntimeState(state);

    await createModerationProductThroughSellerUi(page, state);
  });

  test("buyer confirms receipt and sees the immutable completed-order snapshot", async ({ page }) => {
    const order = requiredLifecycleOrder(state);
    if (!state.lifecycleTrackingNumber) throw new Error("Seller test did not persist the shipment tracking number");
    await login(page, STOREFRONT_URL, state.buyerEmail, state.password);
    await page.goto(`${STOREFRONT_URL}/account/orders/${order.id}`);
    await expect(page.getByRole("heading", { name: `#${order.number}` })).toBeVisible();
    await expect(page.getByText("Đã giao", { exact: true }).first()).toBeVisible();
    await expect(page.getByText(state.lifecycleTrackingNumber, { exact: false })).toBeVisible();
    await expect(page.getByText(state.productName, { exact: true })).toBeVisible();

    const completeResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, `/api/v1/orders/${order.id}/complete`, "POST"),
    );
    await page.getByRole("button", { name: "Xác nhận đã nhận hàng" }).click();
    const completed = await responseData<OrderDetailData>(await completeResponsePromise);
    expect(completed.status).toBe("COMPLETED");
    expect(completed.customerNote).toBe(`Playwright lifecycle ${state.runId}`);
    expect(completed.items).toHaveLength(1);
    expect(completed.items?.[0]).toMatchObject({ productName: state.productName, quantity: 1 });
    expect(completed.items?.[0]?.sku).toBeTruthy();
    expect(completed.items?.[0]?.unitPrice).toBe(state.lifecycleItemsSubtotal);
    expect(completed.shipment).toMatchObject({
      trackingNumber: state.lifecycleTrackingNumber,
      status: "DELIVERED",
    });
    expect(completed.history?.map((entry) => entry.toStatus)).toEqual([
      "CONFIRMED",
      "PROCESSING",
      "READY_TO_SHIP",
      "SHIPPED",
      "DELIVERED",
      "COMPLETED",
    ]);
    await expect(page.getByText("Hoàn tất", { exact: true }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Viết đánh giá" })).toBeVisible();
  });

  test("admin approves the UI-created listing and audits the completed order", async ({ page }) => {
    const order = requiredLifecycleOrder(state);
    const pendingProductId = state.pendingProductId;
    if (!pendingProductId) throw new Error("Seller UI test did not persist the moderation product ID");
    await login(page, ADMIN_URL, state.adminEmail, state.password);
    await expect(page.getByRole("heading", { name: "Tổng quan vận hành" })).toBeVisible();

    await page.goto(`${ADMIN_URL}/products`);
    await expect(page.getByRole("heading", { name: "Duyệt sản phẩm" })).toBeVisible();
    const moderationRow = page.getByRole("row").filter({ hasText: state.pendingProductName });
    await expect(moderationRow).toBeVisible();
    await moderationRow.getByRole("button", { name: "Duyệt" }).click();
    await expect(page.getByRole("dialog", { name: "Phê duyệt sản phẩm" })).toBeVisible();
    const approvalResponsePromise = page.waitForResponse((response) =>
      isApiCall(response, `/api/v1/admin/products/${pendingProductId}/approve`, "POST"),
    );
    await page.getByRole("dialog").getByRole("button", { name: "Xác nhận" }).click();
    const approved = await responseData<ProductData>(await approvalResponsePromise);
    expect(approved.status).toBe("ACTIVE");
    await expect(page.getByText("Sản phẩm đã được duyệt.")).toBeVisible();

    await page.goto(`${ADMIN_URL}/users`);
    await page.getByLabel("Tìm người dùng").fill(state.buyerEmail);
    await page.getByRole("button", { name: "Áp dụng" }).click();
    await expect(page.getByText(state.buyerEmail)).toBeVisible();
    await page.goto(`${ADMIN_URL}/shops`);
    await page.getByLabel("Tìm gian hàng").fill(state.shopName);
    await page.getByRole("button", { name: "Áp dụng" }).click();
    await expect(page.getByText(state.shopName)).toBeVisible();

    await page.goto(`${ADMIN_URL}/orders`);
    await page.getByLabel("Tìm đơn").fill(order.number);
    await page.getByRole("button", { name: "Áp dụng" }).click();
    const adminOrderRow = page.getByRole("row").filter({ hasText: order.number });
    await expect(adminOrderRow).toContainText("Hoàn tất");
    await adminOrderRow.getByRole("button", { name: "Chi tiết" }).click();
    const orderDialog = page.getByRole("dialog", { name: "Chi tiết đơn hàng" });
    await expect(orderDialog).toContainText(state.productName);
    await expect(orderDialog).toContainText(state.lifecycleTrackingNumber!);
    await expect(orderDialog).toContainText("Hoàn tất");
    await expect(orderDialog.getByRole("heading", { name: /Ảnh chụp mặt hàng/ })).toBeVisible();
    await expect(orderDialog.getByRole("heading", { name: "Lịch sử trạng thái" })).toBeVisible();

    await page.goto(`${ADMIN_URL}/audit-logs`);
    await expect(page.getByRole("heading", { name: "Nhật ký kiểm toán" })).toBeVisible();
    const auditItem = page.locator(".audit-timeline li")
      .filter({ hasText: "PRODUCT_APPROVED" })
      .filter({ hasText: `${pendingProductId.slice(0, 8)}…` });
    await expect(auditItem).toBeVisible();
  });
});
