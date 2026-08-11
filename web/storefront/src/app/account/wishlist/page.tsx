"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { EmptyState, ErrorState, Spinner } from "@shoppew/ui";
import { Trash2 } from "lucide-react";
import { ProductCard } from "@/components/product-card";
import { useAuth } from "@/components/providers";
import type { WishlistItem } from "@/lib/types";

export default function WishlistPage() {
  const { request } = useAuth(); const client = useQueryClient(); const query = useQuery({ queryKey: ["wishlist"], queryFn: () => request<WishlistItem[]>("/api/v1/wishlist") });
  const remove = useMutation({ mutationFn: (productId: string) => request<void>(`/api/v1/wishlist/products/${productId}`, { method: "DELETE" }), onSuccess: () => void client.invalidateQueries({ queryKey: ["wishlist"] }) });
  if (query.isPending) return <Spinner label="Đang tải danh sách yêu thích" />; if (query.error) return <ErrorState message={query.error.message} onRetry={() => void query.refetch()} />;
  return <section><div className="section-heading"><div><span className="eyebrow">Đã lưu</span><h1>Sản phẩm yêu thích</h1><p>{query.data?.length ?? 0} sản phẩm</p></div></div>{query.data?.length ? <div className="wishlist-grid">{query.data.map((item) => item.product && <div key={item.id}><ProductCard product={item.product} /><button className="remove-wishlist" onClick={() => item.product?.id && remove.mutate(item.product.id)} disabled={remove.isPending}><Trash2 /> Bỏ lưu</button></div>)}</div> : <EmptyState title="Danh sách yêu thích đang trống" description="Bấm biểu tượng trái tim tại trang sản phẩm để lưu lại." action={<Link className="sp-button" href="/search">Khám phá sản phẩm</Link>} />}</section>;
}
