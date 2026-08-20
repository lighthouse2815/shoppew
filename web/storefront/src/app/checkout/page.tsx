"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Button, ErrorState, Field, Price, Spinner } from "@shoppew/ui";
import { CreditCard, MapPin, ShieldCheck, Truck } from "lucide-react";
import { RequireAuth } from "@/components/require-auth";
import { useAuth } from "@/components/providers";
import type {
  Address,
  Cart,
  CheckoutPreview,
  CheckoutResult,
  CommerceCapabilities,
  PaymentProvider,
} from "@/lib/types";

const paymentCopy: Partial<Record<PaymentProvider, { title: string; description: string }>> = {
  COD: {
    title: "Thanh toán khi nhận hàng",
    description: "Thanh toán trực tiếp khi đơn được giao.",
  },
  MOCK_ONLINE: {
    title: "Cổng thanh toán mô phỏng",
    description: "Chỉ dùng trong môi trường kiểm thử; không phát sinh giao dịch thật.",
  },
};

function paymentDescription(provider: PaymentProvider) {
  return paymentCopy[provider] ?? {
    title: provider,
    description: "Phương thức thanh toán do hệ thống đang cung cấp.",
  };
}

function shippingDescription(methodCode: string) {
  if (methodCode === "MOCK_STANDARD") {
    return {
      title: "Giao hàng tiêu chuẩn mô phỏng",
      description: "Chỉ dùng trong môi trường kiểm thử; không tạo vận đơn thật.",
    };
  }
  return {
    title: methodCode,
    description: "Phương thức vận chuyển do hệ thống đang cung cấp.",
  };
}

export function CheckoutContent() {
  const { request } = useAuth();
  const router = useRouter();
  const [addressId, setAddressId] = useState("");
  const [provider, setProvider] = useState<PaymentProvider>("COD");
  const [shippingMethod, setShippingMethod] = useState("");
  const [voucher, setVoucher] = useState("");
  const [note, setNote] = useState("");

  const cart = useQuery({
    queryKey: ["cart"],
    queryFn: () => request<Cart>("/api/v1/cart"),
  });
  const addresses = useQuery({
    queryKey: ["addresses"],
    queryFn: () => request<Address[]>("/api/v1/users/me/addresses"),
  });
  const capabilities = useQuery({
    queryKey: ["commerce-capabilities"],
    queryFn: () => request<CommerceCapabilities>("/api/v1/public/commerce-capabilities"),
    staleTime: 60_000,
  });

  const selectedIds = useMemo(
    () =>
      cart.data?.shops?.flatMap(
        (shop) =>
          shop.items
            ?.filter((item) => item.selected && item.eligible)
            .flatMap((item) => (item.id ? [item.id] : [])) ?? [],
      ) ?? [],
    [cart.data],
  );
  const selectedAddress =
    addressId || addresses.data?.find((item) => item.defaultAddress)?.id || addresses.data?.[0]?.id || "";
  const paymentProviders = capabilities.data?.availablePaymentProviders ?? [];
  const shippingMethods = capabilities.data?.availableShippingMethods ?? [];
  const selectedProvider = paymentProviders.includes(provider) ? provider : paymentProviders[0] ?? "";
  const selectedShippingMethod = shippingMethods.includes(shippingMethod)
    ? shippingMethod
    : shippingMethods[0] ?? "";
  const body = useMemo(
    () => ({
      cartItemIds: selectedIds,
      addressId: selectedAddress,
      paymentProvider: selectedProvider,
      shippingMethodCode: selectedShippingMethod,
      customerNote: note || undefined,
      voucherCodes: voucher.trim()
        ? voucher
            .split(",")
            .map((code) => code.trim())
            .filter(Boolean)
        : undefined,
    }),
    [note, selectedAddress, selectedIds, selectedProvider, selectedShippingMethod, voucher],
  );
  const preview = useQuery({
    queryKey: ["checkout-preview", body],
    queryFn: () => request<CheckoutPreview>("/api/v1/checkout/preview", { method: "POST", body }),
    enabled:
      selectedIds.length > 0 &&
      Boolean(selectedAddress) &&
      Boolean(selectedProvider) &&
      Boolean(selectedShippingMethod),
    retry: false,
  });
  const place = useMutation({
    mutationFn: () =>
      request<CheckoutResult>("/api/v1/checkout", {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
        body,
      }),
    onSuccess: (result) => {
      const query = new URLSearchParams({
        checkout: result.checkoutNumber ?? "",
        total: String(result.grandTotal ?? 0),
        currency: result.currency ?? "VND",
        orders: result.orders?.map((order) => order.id).filter(Boolean).join(",") ?? "",
      });
      router.replace(`/order/success?${query}`);
    },
  });

  if (cart.isPending || addresses.isPending || capabilities.isPending) {
    return (
      <div className="shell page-section">
        <Spinner label="Đang chuẩn bị thanh toán" />
      </div>
    );
  }
  if (cart.error || addresses.error || capabilities.error) {
    const error = cart.error ?? addresses.error ?? capabilities.error;
    return (
      <div className="shell page-section">
        <ErrorState
          message={error?.message ?? "Không thể chuẩn bị checkout."}
          onRetry={() => {
            void cart.refetch();
            void addresses.refetch();
            void capabilities.refetch();
          }}
        />
      </div>
    );
  }
  if (!selectedIds.length) {
    return (
      <main className="shell narrow-page page-section">
        <div className="notice notice--error">
          Không có sản phẩm hợp lệ được chọn. <Link href="/cart">Quay lại giỏ hàng</Link>.
        </div>
      </main>
    );
  }
  if (!addresses.data?.length) {
    return (
      <main className="shell narrow-page page-section">
        <div className="notice notice--error">
          Bạn cần thêm địa chỉ nhận hàng trước khi thanh toán.
        </div>
        <Link className="sp-button" href="/account/addresses">
          Thêm địa chỉ
        </Link>
      </main>
    );
  }
  if (!paymentProviders.length) {
    return (
      <main className="shell page-section">
        <ErrorState
          message="Chưa có phương thức thanh toán khả dụng. Đơn hàng chưa thể được tạo trong môi trường này."
          onRetry={() => void capabilities.refetch()}
        />
      </main>
    );
  }
  if (!shippingMethods.length) {
    return (
      <main className="shell page-section">
        <ErrorState
          message="Chưa có phương thức vận chuyển khả dụng. Đơn hàng chưa thể được tạo trong môi trường này."
          onRetry={() => void capabilities.refetch()}
        />
      </main>
    );
  }

  return (
    <main className="shell page-section">
      <div className="section-heading">
        <div>
          <span className="eyebrow">Kiểm tra lần cuối</span>
          <h1>Thanh toán</h1>
          <p>Máy chủ sẽ kiểm tra lại giá, tồn kho, vận chuyển và voucher trước khi tạo đơn.</p>
        </div>
      </div>
      <div className="checkout-layout">
        <section className="stack">
          <article className="surface checkout-block">
            <h2>
              <MapPin /> Địa chỉ nhận hàng
            </h2>
            <div className="address-choice">
              {addresses.data.map((address) => (
                <label key={address.id}>
                  <input
                    type="radio"
                    name="address"
                    value={address.id}
                    checked={selectedAddress === address.id}
                    onChange={() => setAddressId(address.id ?? "")}
                  />
                  <span>
                    <strong>
                      {address.recipientName} · {address.phone}
                    </strong>
                    <small>
                      {[address.addressLine, address.ward, address.district, address.province]
                        .filter(Boolean)
                        .join(", ")}
                    </small>
                  </span>
                  {address.defaultAddress && <em>Mặc định</em>}
                </label>
              ))}
            </div>
          </article>

          <article className="surface checkout-block">
            <h2>
              <CreditCard /> Phương thức thanh toán
            </h2>
            {paymentProviders.map((availableProvider) => {
              const copy = paymentDescription(availableProvider);
              return (
                <label className="payment-choice" key={availableProvider}>
                  <input
                    type="radio"
                    name="payment-provider"
                    checked={selectedProvider === availableProvider}
                    onChange={() => setProvider(availableProvider)}
                  />
                  <span>
                    <strong>{copy.title}</strong>
                    <small>{copy.description}</small>
                  </span>
                </label>
              );
            })}
          </article>

          <article className="surface checkout-block">
            <h2>
              <Truck /> Phương thức vận chuyển
            </h2>
            {shippingMethods.map((methodCode) => {
              const copy = shippingDescription(methodCode);
              return (
                <label className="payment-choice" key={methodCode}>
                  <input
                    type="radio"
                    name="shipping-method"
                    checked={selectedShippingMethod === methodCode}
                    onChange={() => setShippingMethod(methodCode)}
                  />
                  <span>
                    <strong>{copy.title}</strong>
                    <small>{copy.description}</small>
                  </span>
                </label>
              );
            })}
          </article>

          <article className="surface form-grid">
            <Field
              label="Mã voucher"
              hint="Có thể nhập nhiều mã, phân cách bằng dấu phẩy."
              value={voucher}
              onChange={(event) => setVoucher(event.target.value)}
            />
            <Field
              label="Ghi chú cho nhà bán"
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </article>
        </section>

        <aside className="surface checkout-summary">
          <h2>Tóm tắt đơn</h2>
          {preview.isFetching && <Spinner label="Đang tính lại tổng" />}
          {preview.error && (
            <div className="stack">
              <p className="notice notice--error">{preview.error.message}</p>
              <Button className="sp-button--secondary" onClick={() => void preview.refetch()}>
                Thử tính lại
              </Button>
            </div>
          )}
          {preview.data && (
            <>
              <div className="checkout-shops">
                {preview.data.shops?.map((shop) => (
                  <div key={shop.shopId}>
                    <strong>{shop.shopName}</strong>
                    <span>{shop.cartItemIds?.length ?? 0} sản phẩm</span>
                    <span>
                      <Truck /> Phí giao: {(shop.shippingFee ?? 0).toLocaleString("vi-VN")} ₫
                    </span>
                  </div>
                ))}
              </div>
              <div className="order-totals">
                <div>
                  <span>Tiền hàng</span>
                  <span>{(preview.data.itemsSubtotal ?? 0).toLocaleString("vi-VN")} ₫</span>
                </div>
                <div>
                  <span>Phí vận chuyển</span>
                  <span>{(preview.data.shippingTotal ?? 0).toLocaleString("vi-VN")} ₫</span>
                </div>
                <div>
                  <span>Giảm giá</span>
                  <span>-{(preview.data.discountTotal ?? 0).toLocaleString("vi-VN")} ₫</span>
                </div>
                <div className="total">
                  <strong>Tổng thanh toán</strong>
                  <Price value={preview.data.grandTotal ?? 0} currency={preview.data.currency} />
                </div>
              </div>
            </>
          )}
          <Button
            disabled={
              !preview.data ||
              preview.isFetching ||
              place.isPending ||
              !selectedProvider ||
              !selectedShippingMethod
            }
            onClick={() => place.mutate()}
          >
            {place.isPending ? "Đang tạo đơn..." : "Đặt hàng"}
          </Button>
          <small>
            <ShieldCheck /> Bằng việc đặt hàng, bạn xác nhận thông tin giao nhận ở trên là chính xác.
          </small>
          {place.error && <p className="notice notice--error">{place.error.message}</p>}
        </aside>
      </div>
    </main>
  );
}

export default function CheckoutPage() {
  return (
    <RequireAuth>
      <CheckoutContent />
    </RequireAuth>
  );
}
