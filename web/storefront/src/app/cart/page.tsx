"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, ShoppingBag, Trash2 } from "lucide-react";
import { EmptyState, ErrorState, Price, Spinner } from "@shoppew/ui";
import { RequireAuth } from "@/components/require-auth";
import { SafeImage } from "@/components/safe-image";
import { useAuth } from "@/components/providers";
import type { Cart } from "@/lib/types";

function CartContent() {
  const { request } = useAuth();
  const client = useQueryClient();
  const refresh = () => client.invalidateQueries({ queryKey: ["cart"] });
  const cart = useQuery({ queryKey: ["cart"], queryFn: () => request<Cart>("/api/v1/cart") });
  const update = useMutation({ mutationFn: ({ id, quantity }: { id: string; quantity: number }) => request<Cart>(`/api/v1/cart/items/${id}`, { method: "PUT", body: { quantity } }), onSuccess: () => void refresh() });
  const select = useMutation({ mutationFn: ({ id, selected }: { id: string; selected: boolean }) => request<Cart>(`/api/v1/cart/items/${id}/selection`, { method: "PATCH", body: { selected } }), onSuccess: () => void refresh() });
  const selectAll = useMutation({ mutationFn: ({ ids, selected }: { ids: string[]; selected: boolean }) => request<Cart>("/api/v1/cart/selection", { method: "PUT", body: { itemIds: ids, selected } }), onSuccess: () => void refresh() });
  const remove = useMutation({ mutationFn: (id: string) => request<Cart>(`/api/v1/cart/items/${id}`, { method: "DELETE" }), onSuccess: () => void refresh() });

  if (cart.isPending) return <div className="shell page-section"><Spinner label="Đang kiểm tra giỏ hàng" /></div>;
  if (cart.error) return <div className="shell page-section"><ErrorState message={cart.error.message} onRetry={() => void cart.refetch()} /></div>;

  const items = cart.data?.shops?.flatMap((shop) => shop.items ?? []) ?? [];
  const ids = items.flatMap((item) => item.id ? [item.id] : []);
  const allSelected = items.length > 0 && items.every((item) => item.selected);
  const mutationError = update.error ?? select.error ?? selectAll.error ?? remove.error;

  return (
    <main className="shell page-section">
      <div className="section-heading"><div><span className="eyebrow">Giỏ hàng của bạn</span><h1>Giỏ hàng</h1><p>{cart.data?.itemCount ?? 0} sản phẩm từ {cart.data?.shops?.length ?? 0} shop</p></div></div>
      {items.length ? (
        <div className="cart-layout">
          <section className="cart-list">
            <label className="cart-select-all"><input type="checkbox" checked={allSelected} disabled={selectAll.isPending} onChange={(event) => selectAll.mutate({ ids, selected: event.target.checked })} /> Chọn tất cả sản phẩm</label>
            {cart.data?.shops?.map((shop) => (
              <article className="surface cart-shop" key={shop.shopId}>
                <h2><Link href={`/shop/${shop.shopSlug}`}>{shop.shopName}</Link></h2>
                {shop.items?.map((item) => (
                  <div className={`cart-item ${!item.eligible ? "cart-item--invalid" : ""}`} key={item.id}>
                    <input type="checkbox" checked={Boolean(item.selected)} disabled={!item.eligible || select.isPending} aria-label={`Chọn ${item.productName}`} onChange={(event) => item.id && select.mutate({ id: item.id, selected: event.target.checked })} />
                    <span className="cart-item__image"><SafeImage src={item.imageUrl ?? ""} alt={item.productName ?? "Sản phẩm"} fill sizes="92px" /></span>
                    <div className="cart-item__info"><Link href={`/product/${item.productSlug}`}>{item.productName}</Link><span>{item.variantName || item.sku}</span><Price value={item.unitPrice ?? 0} currency={item.currency} compareAt={item.originalUnitPrice} />{item.issues?.map((issue) => <small className="danger-text" key={issue}>{issue}</small>)}</div>
                    <div className="cart-quantity"><button onClick={() => item.id && update.mutate({ id: item.id, quantity: Math.max(1, (item.quantity ?? 1) - 1) })} disabled={update.isPending || (item.quantity ?? 1) <= 1} aria-label="Giảm số lượng"><Minus /></button><span>{item.quantity}</span><button onClick={() => item.id && update.mutate({ id: item.id, quantity: (item.quantity ?? 1) + 1 })} disabled={update.isPending || (item.quantity ?? 1) >= (item.availableQuantity ?? 0)} aria-label="Tăng số lượng"><Plus /></button></div>
                    <strong className="cart-line-total">{(item.lineTotal ?? 0).toLocaleString("vi-VN")} ₫</strong>
                    <button className="cart-remove" aria-label={`Xóa ${item.productName}`} onClick={() => item.id && confirm("Xóa sản phẩm khỏi giỏ?") && remove.mutate(item.id)} disabled={remove.isPending}><Trash2 /></button>
                  </div>
                ))}
              </article>
            ))}
          </section>
          <aside className="surface cart-summary"><h2>Tóm tắt</h2><div><span>Đã chọn</span><strong>{cart.data?.selectedItemCount ?? 0} sản phẩm</strong></div><div className="total"><span>Tạm tính</span><Price value={cart.data?.selectedSubtotal ?? 0} currency={cart.data?.currency} /></div><Link className={`sp-button ${(cart.data?.selectedItemCount ?? 0) < 1 ? "disabled" : ""}`} aria-disabled={(cart.data?.selectedItemCount ?? 0) < 1} href={(cart.data?.selectedItemCount ?? 0) > 0 ? "/checkout" : "/cart"}>Tiến hành thanh toán</Link><small>Phí vận chuyển, voucher và tổng cuối cùng sẽ được máy chủ tính lại.</small></aside>
        </div>
      ) : <EmptyState title="Giỏ hàng đang trống" description="Hãy thêm sản phẩm và quay lại để thanh toán." action={<Link className="sp-button" href="/search"><ShoppingBag /> Khám phá sản phẩm</Link>} />}
      {mutationError && <p className="notice notice--error">{mutationError.message}</p>}
    </main>
  );
}

export default function CartPage() { return <RequireAuth><CartContent /></RequireAuth>; }
