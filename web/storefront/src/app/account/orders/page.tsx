"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { EmptyState, ErrorState, Price, Spinner } from "@shoppew/ui";
import { ChevronRight, Package } from "lucide-react";
import { useAuth } from "@/components/providers";
import { formatDateTime, orderStatusLabel } from "@/lib/format";
import type { OrderSummary, Page } from "@/lib/types";

export default function OrdersPage() {
  const { request } = useAuth();
  const orders = useQuery({ queryKey: ["orders"], queryFn: () => request<Page<OrderSummary>>("/api/v1/orders?size=50") });
  if (orders.isPending) return <Spinner label="Đang tải đơn mua" />;
  if (orders.error) return <ErrorState message={orders.error.message} onRetry={() => void orders.refetch()} />;
  return <section><div className="section-heading"><div><span className="eyebrow">Lịch sử mua sắm</span><h1>Đơn mua</h1><p>{orders.data?.totalElements ?? 0} đơn hàng</p></div></div>{orders.data?.content?.length ? <div className="order-list">{orders.data.content.map((order) => <Link href={`/account/orders/${order.id}`} className="surface order-row" key={order.id}><Package /><div><strong>#{order.orderNumber}</strong><span>{order.shopName} · {order.itemCount ?? 0} sản phẩm</span><time>{formatDateTime(order.placedAt)}</time></div><div><span className="status-pill">{orderStatusLabel[order.status ?? ""] ?? order.status}</span><Price value={order.grandTotal ?? 0} currency={order.currency} /><ChevronRight /></div></Link>)}</div> : <EmptyState title="Bạn chưa có đơn mua" description="Sau khi checkout thành công, đơn hàng sẽ xuất hiện tại đây." action={<Link className="sp-button" href="/search">Khám phá sản phẩm</Link>} />}</section>;
}
