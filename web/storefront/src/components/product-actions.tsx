"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Heart, ShoppingBag } from "lucide-react";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Button, Price } from "@shoppew/ui";
import type { ProductDetail } from "@/lib/types";
import { useAuth } from "./providers";

export function ProductActions({ product }: { product: ProductDetail }) {
  const { status, request } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const available = useMemo(() => product.variants?.filter((variant) => variant.status === "ACTIVE") ?? [], [product.variants]);
  const [variantId, setVariantId] = useState(available[0]?.id ?? "");
  const [quantity, setQuantity] = useState(1);
  const variant = available.find((item) => item.id === variantId);

  const requireLogin = () => {
    if (status !== "authenticated") {
      router.push(`/login?returnTo=${encodeURIComponent(`/product/${product.slug}`)}`);
      return false;
    }
    return true;
  };
  const addCart = useMutation({
    mutationFn: async () => {
      if (!requireLogin() || !variantId) throw new Error("Vui lòng chọn phân loại sản phẩm.");
      return request("/api/v1/cart/items", { method: "POST", body: { variantId, quantity } });
    },
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["cart"] }); },
  });
  const addWishlist = useMutation({
    mutationFn: async () => {
      if (!requireLogin() || !product.id) throw new Error("Bạn cần đăng nhập.");
      return request(`/api/v1/wishlist/products/${product.id}`, { method: "POST" });
    },
  });

  return (
    <div className="purchase-panel">
      <Price value={variant?.price ?? 0} currency={variant?.currency} compareAt={variant?.compareAtPrice ?? variant?.originalPrice} />
      {product.options?.map((option) => <div className="option-row" key={option.id}><strong>{option.name}</strong><div>{option.values?.map((value) => {
        const candidate = available.find((item) => item.selections?.some((selection) => selection.valueId === value.id));
        return <button key={value.id} type="button" className={variant?.selections?.some((selection) => selection.valueId === value.id) ? "active" : ""} disabled={!candidate} onClick={() => candidate?.id && setVariantId(candidate.id)}>{value.value}</button>;
      })}</div></div>)}
      {available.length > 1 && !product.options?.length && <label>Phân loại<select className="form-control" value={variantId} onChange={(event) => setVariantId(event.target.value)}>{available.map((item) => <option key={item.id} value={item.id}>{item.name ?? item.sku}</option>)}</select></label>}
      <label className="quantity-control">Số lượng<input className="form-control" type="number" min={1} max={99} value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} /></label>
      {available.length ? <div className="purchase-actions"><Button type="button" onClick={() => addCart.mutate()} disabled={addCart.isPending}><ShoppingBag /> {addCart.isPending ? "Đang thêm..." : "Thêm vào giỏ"}</Button><Button type="button" className="sp-button--secondary" onClick={() => addWishlist.mutate()} disabled={addWishlist.isPending}><Heart /> Yêu thích</Button></div> : <p className="notice notice--error">Sản phẩm hiện không có phân loại khả dụng.</p>}
      {addCart.isSuccess && <p className="notice notice--success">Đã thêm sản phẩm vào giỏ hàng.</p>}
      {addWishlist.isSuccess && <p className="notice notice--success">Đã lưu sản phẩm vào danh sách yêu thích.</p>}
      {(addCart.error || addWishlist.error) && <p className="notice notice--error">{(addCart.error ?? addWishlist.error)?.message}</p>}
    </div>
  );
}
