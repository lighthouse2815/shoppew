"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button, ErrorState, Field, Price, Spinner } from "@shoppew/ui";
import { CheckCircle2, MapPin, PackageCheck, Truck } from "lucide-react";
import { useAuth } from "./providers";
import { SafeImage } from "./safe-image";
import { formatDateTime, orderStatusLabel } from "@/lib/format";
import type { OrderDetail, Review } from "@/lib/types";

export function OrderDetailView({ orderId }: { orderId: string }) {
  const { request } = useAuth();
  const client = useQueryClient();
  const [reason, setReason] = useState("");
  const [reviewing, setReviewing] = useState<string | null>(null);
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const query = useQuery({ queryKey: ["order", orderId], queryFn: () => request<OrderDetail>(`/api/v1/orders/${orderId}`) });
  const refresh = () => client.invalidateQueries({ queryKey: ["order", orderId] });
  const command = useMutation({
    mutationFn: ({ action, body }: { action: "cancel" | "complete"; body?: unknown }) => request<OrderDetail>(`/api/v1/orders/${orderId}/${action}`, { method: "POST", body }),
    onSuccess: () => void refresh(),
  });
  const review = useMutation({
    mutationFn: (orderItemId: string) => request<Review>("/api/v1/reviews", { method: "POST", body: { orderItemId, rating, content: content || undefined } }),
    onSuccess: () => { setReviewing(null); setContent(""); },
  });

  if (query.isPending) return <Spinner label="Đang tải chi tiết đơn" />;
  if (query.error || !query.data) return <ErrorState message={query.error?.message ?? "Không tìm thấy đơn hàng."} onRetry={() => void query.refetch()} />;
  const order = query.data;

  return (
    <section>
      <nav className="breadcrumbs"><Link href="/account/orders">Đơn mua</Link><span>/</span><span>#{order.orderNumber}</span></nav>
      <div className="order-detail-head"><div><span className="eyebrow">Đơn hàng</span><h1>#{order.orderNumber}</h1><p>Đặt lúc {formatDateTime(order.placedAt)}</p></div><span className="status-pill status-pill--large">{orderStatusLabel[order.status ?? ""] ?? order.status}</span></div>
      <div className="order-detail-grid">
        <div className="stack">
          <article className="surface">
            <h2>Sản phẩm từ {order.shopName}</h2>
            <div className="order-items">
              {order.items?.map((item) => (
                <div key={item.id}>
                  <span className="order-item__image"><SafeImage src={item.imageUrl ?? ""} alt={item.productName ?? "Sản phẩm"} fill sizes="76px" /></span>
                  <div><strong>{item.productName}</strong><span>{item.variantName ?? item.sku}</span><span>x{item.quantity}</span>{order.status === "COMPLETED" && item.id && <button className="text-link" onClick={() => setReviewing(item.id!)}>Viết đánh giá</button>}</div>
                  <Price value={item.subtotal ?? 0} currency={item.currency} />
                </div>
              ))}
            </div>
          </article>
          {reviewing && (
            <form className="surface form-grid" method="post" onSubmit={(event) => { event.preventDefault(); review.mutate(reviewing); }}>
              <h2 className="full">Đánh giá sản phẩm</h2>
              <label className="sp-field"><span className="sp-field__label">Số sao</span><select className="form-control" value={rating} onChange={(event) => setRating(Number(event.target.value))}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} sao</option>)}</select></label>
              <Field className="full" label="Nhận xét" value={content} onChange={(event) => setContent(event.target.value)} />
              <div className="full form-actions"><Button disabled={review.isPending}>{review.isPending ? "Đang gửi..." : "Gửi đánh giá"}</Button><Button type="button" className="sp-button--secondary" onClick={() => setReviewing(null)}>Hủy</Button></div>
              {review.error && <p className="full notice notice--error">{review.error.message}</p>}
            </form>
          )}
          <article className="surface"><h2>Tiến trình đơn hàng</h2><ol className="timeline">{order.history?.map((entry, index) => <li key={`${entry.createdAt}-${index}`}><CheckCircle2 /><div><strong>{orderStatusLabel[entry.toStatus ?? ""] ?? entry.toStatus}</strong><time>{formatDateTime(entry.createdAt)}</time>{entry.reason && <p>{entry.reason}</p>}</div></li>)}</ol></article>
        </div>
        <aside className="stack">
          <article className="surface"><h2><MapPin /> Địa chỉ giao hàng</h2><strong>{order.address?.recipientName}</strong><p>{order.address?.phone}</p><p className="muted">{[order.address?.addressLine, order.address?.ward, order.address?.district, order.address?.province].filter(Boolean).join(", ")}</p></article>
          <article className="surface order-totals"><h2>Thanh toán</h2><div><span>Tiền hàng</span><span>{order.itemsSubtotal?.toLocaleString("vi-VN")} ₫</span></div><div><span>Vận chuyển</span><span>{order.shippingTotal?.toLocaleString("vi-VN")} ₫</span></div><div><span>Giảm giá</span><span>-{((order.shopDiscountTotal ?? 0) + (order.platformDiscountTotal ?? 0)).toLocaleString("vi-VN")} ₫</span></div><div className="total"><strong>Tổng cộng</strong><Price value={order.grandTotal ?? 0} currency={order.currency} /></div></article>
          {order.shipment && <article className="surface"><h2><Truck /> Vận chuyển</h2><p>{order.shipment.provider} · {order.shipment.trackingNumber || "Chưa có mã vận đơn"}</p></article>}
          {["PENDING_PAYMENT", "PAID", "CONFIRMED"].includes(order.status ?? "") && <article className="surface stack"><h2>Hủy đơn</h2><Field label="Lý do" value={reason} onChange={(event) => setReason(event.target.value)} /><Button className="sp-button--danger" disabled={command.isPending || !reason.trim()} onClick={() => command.mutate({ action: "cancel", body: { reason } })}>Hủy đơn hàng</Button></article>}
          {order.status === "DELIVERED" && <Button disabled={command.isPending} onClick={() => command.mutate({ action: "complete" })}><PackageCheck /> Xác nhận đã nhận hàng</Button>}
          {command.error && <p className="notice notice--error">{command.error.message}</p>}
        </aside>
      </div>
    </section>
  );
}
