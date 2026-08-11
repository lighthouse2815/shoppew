import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowRight, CircleDollarSign, Package, TriangleAlert } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Field } from "@shoppew/ui";
import { ErrorBlock, Loading, PageHeader, Status } from "@/components/common";
import { useAuth, useShop } from "@/providers";
import { money } from "@/lib/format";
import type { Analytics, Balance, Inventory, OrderSummary, Page, Shop } from "@/lib/types";

export function DashboardPage() {
  const { shop, error, refreshShops } = useShop();
  if (error) return <ErrorBlock error={error} retry={() => void refreshShops()} />;
  if (!shop) return <Onboarding onCreated={refreshShops} />;
  return <Dashboard shop={shop} />;
}

function Onboarding({ onCreated }: { onCreated: () => Promise<unknown> }) {
  const { request } = useAuth(); const [form, setForm] = useState({ name: "", slug: "", description: "" });
  const create = useMutation({ mutationFn: () => request<Shop>("/api/v1/seller/shops", { method: "POST", body: { name: form.name, slug: form.slug || undefined, description: form.description || undefined } }), onSuccess: async () => { await onCreated(); } });
  return <div className="onboarding"><span className="onboarding-index">01</span><div><span className="page-kicker">Khởi tạo Seller Center</span><h1>Tạo gian hàng đầu tiên</h1><p>Gian hàng mới cần được quản trị viên duyệt trước khi sản phẩm có thể xuất hiện trên storefront.</p><form onSubmit={(event) => { event.preventDefault(); create.mutate(); }}><Field label="Tên gian hàng" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><Field label="Slug (không bắt buộc)" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} /><label><span>Mô tả</span><textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>{create.error && <p className="form-error">{create.error.message}</p>}<Button disabled={create.isPending || !form.name.trim()}>{create.isPending ? "Đang tạo..." : "Tạo gian hàng"}</Button></form></div></div>;
}

function Dashboard({ shop }: { shop: Shop }) {
  const { request } = useAuth(); const id = shop.id!;
  const analytics = useQuery({ queryKey: ["seller-analytics", id], queryFn: () => request<Analytics>(`/api/v1/seller/shops/${id}/analytics`) });
  const balance = useQuery({ queryKey: ["seller-balance", id], queryFn: () => request<Balance>(`/api/v1/seller/shops/${id}/finance/balance`) });
  const orders = useQuery({ queryKey: ["seller-orders", id], queryFn: () => request<Page<OrderSummary>>(`/api/v1/seller/shops/${id}/orders?size=5`) });
  const inventory = useQuery({ queryKey: ["seller-inventory-low", id], queryFn: () => request<Page<Inventory>>(`/api/v1/seller/shops/${id}/inventory?lowStockOnly=true&size=5`) });
  if (analytics.isPending || balance.isPending || orders.isPending || inventory.isPending) return <Loading label="Đang tổng hợp vận hành" />;
  const error = analytics.error ?? balance.error ?? orders.error ?? inventory.error; if (error) return <ErrorBlock error={error} />;
  return <><PageHeader eyebrow="Control room" title="Tổng quan vận hành" description={`${shop.name} · dữ liệu cập nhật từ backend`} action={<Status value={shop.status} />} /><div className="metric-grid"><article><CircleDollarSign /><span>Doanh thu hoàn tất</span><strong>{money(analytics.data?.revenue)}</strong><small>{analytics.data?.completedOrders ?? 0} đơn hoàn tất</small></article><article><Package /><span>Giá trị đơn trung bình</span><strong>{money(analytics.data?.averageOrderValue)}</strong><small>Trên các đơn hoàn tất</small></article><article><CircleDollarSign /><span>Số dư khả dụng</span><strong>{money(balance.data?.availableAmount, balance.data?.currency)}</strong><small>Chờ xử lý: {money(balance.data?.pendingAmount, balance.data?.currency)}</small></article><article className={(inventory.data?.totalElements ?? 0) > 0 ? "metric-alert" : ""}><TriangleAlert /><span>Sắp hết hàng</span><strong>{inventory.data?.totalElements ?? 0}</strong><small>Biến thể dưới ngưỡng</small></article></div><div className="dashboard-grid"><section className="panel"><div className="panel-head"><h2>Đơn mới nhất</h2><Link to="/orders">Xem tất cả <ArrowRight /></Link></div>{orders.data?.content?.length ? <div className="compact-list">{orders.data.content.map((order) => <Link key={order.id} to={`/orders/${order.id}`}><div><strong>#{order.orderNumber}</strong><span>{order.itemCount} sản phẩm</span></div><div><Status value={order.status} /><strong>{money(order.grandTotal, order.currency)}</strong></div></Link>)}</div> : <p className="inline-empty">Chưa có đơn hàng.</p>}</section><section className="panel"><div className="panel-head"><h2>Cảnh báo tồn kho</h2><Link to="/inventory">Mở kho <ArrowRight /></Link></div>{inventory.data?.content?.length ? <div className="compact-list">{inventory.data.content.map((item) => <div key={item.variantId}><div><strong>{item.productName}</strong><span>{item.variantName} · {item.sku}</span></div><strong>{item.availableQuantity ?? 0}</strong></div>)}</div> : <p className="inline-empty">Không có biến thể dưới ngưỡng.</p>}</section></div></>;
}
