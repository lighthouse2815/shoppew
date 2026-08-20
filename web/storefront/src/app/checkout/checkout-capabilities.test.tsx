import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CheckoutContent } from "./page";

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

vi.mock("@/components/providers", () => ({
  useAuth: () => ({ request: mocks.request }),
}));

vi.mock("@/components/require-auth", () => ({
  RequireAuth: ({ children }: { children: ReactNode }) => children,
}));

function renderCheckout() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <CheckoutContent />
    </QueryClientProvider>,
  );
}

function arrangeRequest(capabilities: {
  availablePaymentProviders: string[];
  availableShippingMethods: string[];
}) {
  mocks.request.mockImplementation((path: string) => {
    if (path === "/api/v1/cart") {
      return Promise.resolve({
        shops: [{ items: [{ id: "cart-item-1", selected: true, eligible: true }] }],
      });
    }
    if (path === "/api/v1/users/me/addresses") {
      return Promise.resolve([
        {
          id: "address-1",
          recipientName: "Nguyễn An",
          phone: "0900000000",
          province: "TP. Hồ Chí Minh",
          district: "Quận 1",
          addressLine: "12 Nguyễn Huệ",
          defaultAddress: true,
        },
      ]);
    }
    if (path === "/api/v1/public/commerce-capabilities") {
      return Promise.resolve(capabilities);
    }
    if (path === "/api/v1/checkout/preview") {
      return Promise.resolve({
        shops: [],
        itemsSubtotal: 100_000,
        shippingTotal: 22_000,
        discountTotal: 0,
        grandTotal: 122_000,
        currency: "VND",
      });
    }
    throw new Error(`Unexpected request: ${path}`);
  });
}

describe("checkout commerce capabilities", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.replace.mockReset();
  });

  it("does not render the local mock payment when the backend only enables COD", async () => {
    arrangeRequest({
      availablePaymentProviders: ["COD"],
      availableShippingMethods: ["MOCK_STANDARD"],
    });

    renderCheckout();

    expect(await screen.findByText("Thanh toán khi nhận hàng")).toBeInTheDocument();
    expect(screen.queryByText("Cổng thanh toán mô phỏng")).not.toBeInTheDocument();
    await waitFor(() =>
      expect(mocks.request).toHaveBeenCalledWith(
        "/api/v1/checkout/preview",
        expect.objectContaining({
          body: expect.objectContaining({
            paymentProvider: "COD",
            shippingMethodCode: "MOCK_STANDARD",
          }),
        }),
      ),
    );
  });

  it("blocks checkout with a recoverable state when no payment provider is enabled", async () => {
    arrangeRequest({
      availablePaymentProviders: [],
      availableShippingMethods: ["MOCK_STANDARD"],
    });

    renderCheckout();

    expect(
      await screen.findByText(
        "Chưa có phương thức thanh toán khả dụng. Đơn hàng chưa thể được tạo trong môi trường này.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /thử lại/i })).toBeInTheDocument();
  });

  it("blocks checkout when no shipping method is enabled", async () => {
    arrangeRequest({
      availablePaymentProviders: ["COD"],
      availableShippingMethods: [],
    });

    renderCheckout();

    expect(
      await screen.findByText(
        "Chưa có phương thức vận chuyển khả dụng. Đơn hàng chưa thể được tạo trong môi trường này.",
      ),
    ).toBeInTheDocument();
  });
});
