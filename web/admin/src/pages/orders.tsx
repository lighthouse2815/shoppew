import { useQuery } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Eye, PackageCheck, Search } from "lucide-react";
import { useState } from "react";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, PageHeader, Pagination, SelectField, StatusBadge } from "@/components/common";
import { buildAdminQuery } from "@/lib/admin";
import { formatBusinessTime, formatMoney, formatNumber, shortId } from "@/lib/format";
import type { AdminOrderDetail, AdminOrderPage, OrderDetail } from "@/lib/types";
import { useAuth } from "@/providers";

const emptyFilters = { query: "", status: "", shopId: "", userId: "" };

export function OrdersPage() {
  const { request } = useAuth();
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState("");
  const ordersQuery = useQuery({
    queryKey: ["admin-orders", filters, page],
    queryFn: () => request<AdminOrderPage>(`/api/v1/admin/orders${buildAdminQuery({ ...filters, page, size: 20 })}`),
  });
  const detailQuery = useQuery({
    queryKey: ["admin-order", detailId],
    queryFn: () => request<AdminOrderDetail>(`/api/v1/admin/orders/${detailId}`),
    enabled: Boolean(detailId),
  });
  const orders = ordersQuery.data?.content ?? [];

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setPage(0);
    setFilters({ query: draft.query.trim(), status: draft.status, shopId: draft.shopId.trim(), userId: draft.userId.trim() });
  }

  return (
    <>
      <PageHeader eyebrow="Order oversight" title="Đơn hàng" description="Quan sát đơn theo khách hàng và gian hàng, kiểm tra ảnh chụp mặt hàng bất biến, thanh toán, giao nhận và lịch sử trạng thái." />
      <form className="filter-bar filter-bar--wide" onSubmit={applyFilters}>
        <Field label="Tìm đơn" value={draft.query} onChange={(event) => setDraft({ ...draft, query: event.target.value })} placeholder="Mã đơn, email hoặc tên shop" />
        <SelectField label="Trạng thái" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}>
          <option value="">Tất cả</option><option value="PENDING_PAYMENT">Chờ thanh toán</option><option value="PAID">Đã thanh toán</option><option value="CONFIRMED">Đã xác nhận</option><option value="PROCESSING">Đang xử lý</option><option value="READY_TO_SHIP">Chờ lấy hàng</option><option value="SHIPPED">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="COMPLETED">Hoàn tất</option><option value="CANCELLED">Đã hủy</option><option value="REFUND_REQUESTED">Yêu cầu hoàn</option><option value="PARTIALLY_REFUNDED">Hoàn một phần</option><option value="REFUNDED">Đã hoàn</option>
        </SelectField>
        <Field label="ID gian hàng" value={draft.shopId} onChange={(event) => setDraft({ ...draft, shopId: event.target.value })} placeholder="Lọc chính xác nếu cần" />
        <Field label="ID người dùng" value={draft.userId} onChange={(event) => setDraft({ ...draft, userId: event.target.value })} placeholder="Lọc chính xác nếu cần" />
        <Button disabled={ordersQuery.isFetching}><Search aria-hidden="true" /> {ordersQuery.isFetching ? "Đang tìm…" : "Áp dụng"}</Button>
        {(Object.values(draft).some(Boolean) || Object.values(filters).some(Boolean)) ? <Button className="button-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(0); }}>Xóa lọc</Button> : null}
      </form>
      {ordersQuery.isPending ? <LoadingPanel rows={9} label="Đang tải danh sách đơn hàng" /> : ordersQuery.isError ? <ErrorPanel error={ordersQuery.error} onRetry={() => void ordersQuery.refetch()} /> : orders.length === 0 ? <EmptyPanel title="Không tìm thấy đơn hàng" description="Không có đơn nào phù hợp với bộ lọc hiện tại." /> : (
        <section className="panel table-panel">
          <div className="data-table admin-order-table" role="table" aria-label="Danh sách đơn hàng">
            <div className="data-row data-row--head" role="row"><span>Đơn hàng</span><span>Khách hàng</span><span>Gian hàng</span><span>Mặt hàng</span><span>Thành tiền</span><span>Trạng thái</span><span>Hành động</span></div>
            {orders.map((order) => <article className="data-row" role="row" key={order.id}>
              <div data-label="Đơn hàng"><strong>{order.orderNumber || shortId(order.id)}</strong><small>{formatBusinessTime(order.placedAt)}</small></div>
              <div data-label="Khách hàng"><span>{order.customerEmail || "Không có email"}</span><small>{shortId(order.userId)}</small></div>
              <div data-label="Gian hàng"><strong>{order.shopName || "Gian hàng"}</strong><small>{shortId(order.shopId)}</small></div>
              <span data-label="Mặt hàng" className="numeric">{formatNumber(order.itemCount)}</span>
              <span data-label="Thành tiền" className="numeric strong-money">{formatMoney(order.grandTotal, order.currency)}</span>
              <span data-label="Trạng thái"><StatusBadge value={order.status} /></span>
              <div data-label="Hành động" className="row-actions"><Button className="button-quiet" type="button" onClick={() => setDetailId(order.id ?? "")}><Eye aria-hidden="true" /> Chi tiết</Button></div>
            </article>)}
          </div>
          <Pagination page={page} totalPages={ordersQuery.data?.totalPages ?? 1} onChange={setPage} disabled={ordersQuery.isFetching} />
        </section>
      )}

      <Dialog open={Boolean(detailId)} title="Chi tiết đơn hàng" description={detailId ? `ID ${shortId(detailId)}` : undefined} onClose={() => setDetailId("")}>
        <div className="dialog-body dialog-body--order">
          {detailQuery.isPending ? <LoadingPanel rows={10} /> : detailQuery.isError ? <ErrorPanel error={detailQuery.error} onRetry={() => void detailQuery.refetch()} /> : detailQuery.data?.order ? <OrderDetailView detail={detailQuery.data} /> : null}
        </div>
      </Dialog>
    </>
  );
}

function OrderDetailView({ detail }: { detail: AdminOrderDetail }) {
  const order = detail.order as OrderDetail;
  return (
    <>
      <div className="entity-heading"><div><strong>{order.orderNumber || shortId(order.id)}</strong><span>{order.shopName || "Gian hàng"} · {detail.customerEmail || shortId(detail.userId)}</span></div><StatusBadge value={order.status} /></div>
      <dl className="detail-grid detail-grid--three">
        <div><dt>Tạm tính</dt><dd>{formatMoney(order.itemsSubtotal, order.currency)}</dd></div><div><dt>Vận chuyển</dt><dd>{formatMoney(order.shippingTotal, order.currency)}</dd></div><div><dt>Giảm từ shop</dt><dd>{formatMoney(order.shopDiscountTotal, order.currency)}</dd></div>
        <div><dt>Giảm nền tảng</dt><dd>{formatMoney(order.platformDiscountTotal, order.currency)}</dd></div><div><dt>Tổng thanh toán</dt><dd className="strong-money">{formatMoney(order.grandTotal, order.currency)}</dd></div><div><dt>Đặt lúc</dt><dd>{formatBusinessTime(order.placedAt)}</dd></div>
      </dl>
      <section className="dialog-section"><h3><PackageCheck aria-hidden="true" /> Ảnh chụp mặt hàng ({order.items?.length ?? 0})</h3>{order.items?.length ? <ul className="order-item-list">{order.items.map((item) => <li key={item.id}><div className="order-item-media">{item.imageUrl ? <img src={item.imageUrl} alt="" loading="lazy" decoding="async" /> : <PackageCheck aria-hidden="true" />}</div><span><strong>{item.productName || "Sản phẩm"}</strong><small>{item.variantName || item.sku || "Biến thể mặc định"}</small></span><span>{formatNumber(item.quantity)} × {formatMoney(item.unitPrice, item.currency)}</span><strong>{formatMoney(item.subtotal, item.currency)}</strong></li>)}</ul> : <p className="muted">Không có mặt hàng trong phản hồi.</p>}</section>
      <section className="dialog-section"><h3>Địa chỉ giao hàng</h3>{order.address ? <address>{order.address.recipientName} · {order.address.phone}<br />{[order.address.addressLine, order.address.ward, order.address.district, order.address.province].filter(Boolean).join(", ")}</address> : <p className="muted">Không có địa chỉ.</p>}{order.customerNote ? <p><strong>Ghi chú:</strong> {order.customerNote}</p> : null}</section>
      <section className="dialog-section"><h3>Thanh toán</h3>{detail.payment ? <dl className="detail-grid"><div><dt>Nhà cung cấp</dt><dd>{detail.payment.provider || "—"}</dd></div><div><dt>Trạng thái</dt><dd><StatusBadge value={detail.payment.status} /></dd></div><div><dt>Mã tham chiếu</dt><dd className="mono">{detail.payment.providerReference || "—"}</dd></div><div><dt>Thanh toán lúc</dt><dd>{formatBusinessTime(detail.payment.paidAt)}</dd></div></dl> : <p className="muted">Đơn chưa có bản ghi thanh toán.</p>}</section>
      <section className="dialog-section"><h3>Giao nhận</h3>{order.shipment ? <dl className="detail-grid"><div><dt>Đơn vị</dt><dd>{order.shipment.provider || "—"}</dd></div><div><dt>Dịch vụ</dt><dd>{order.shipment.methodName || order.shipment.methodCode || "—"}</dd></div><div><dt>Mã vận đơn</dt><dd className="mono">{order.shipment.trackingNumber || "—"}</dd></div><div><dt>Trạng thái</dt><dd><StatusBadge value={order.shipment.status} /></dd></div></dl> : <p className="muted">Chưa tạo vận đơn.</p>}</section>
      <section className="dialog-section"><h3>Lịch sử trạng thái</h3>{order.history?.length ? <ol className="compact-timeline">{order.history.map((entry, index) => <li key={`${entry.createdAt}-${index}`}><span><StatusBadge value={entry.toStatus} /><small>{entry.fromStatus ? `Từ ${entry.fromStatus}` : "Khởi tạo"}</small></span><p>{entry.reason || "Không có ghi chú."}</p><time dateTime={entry.createdAt}>{formatBusinessTime(entry.createdAt)}</time></li>)}</ol> : <p className="muted">Chưa có lịch sử.</p>}</section>
    </>
  );
}
