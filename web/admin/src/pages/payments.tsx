import { useQuery } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Eye, Search } from "lucide-react";
import { useState } from "react";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, SelectField, StatusBadge } from "@/components/common";
import { buildAdminQuery } from "@/lib/admin";
import { formatBusinessTime, formatMoney, shortId } from "@/lib/format";
import type { AdminPaymentPage, AdminPaymentSummary } from "@/lib/types";
import { useAuth } from "@/providers";

const emptyFilters = { query: "", status: "", provider: "" };

export function PaymentsPage() {
  const { request } = useAuth();
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState("");
  const paymentsQuery = useQuery({
    queryKey: ["admin-payments", filters, page],
    queryFn: () => request<AdminPaymentPage>(`/api/v1/admin/payments${buildAdminQuery({ ...filters, page, size: 20 })}`),
  });
  const detailQuery = useQuery({
    queryKey: ["admin-payment", detailId],
    queryFn: () => request<AdminPaymentSummary>(`/api/v1/admin/payments/${detailId}`),
    enabled: Boolean(detailId),
  });
  const payments = paymentsQuery.data?.content ?? [];

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setPage(0);
    setFilters({ query: draft.query.trim(), status: draft.status, provider: draft.provider });
  }

  return (
    <>
      <PageHeader eyebrow="Payment observability" title="Thanh toán" description="Đối soát giao dịch theo checkout, khách hàng, nhà cung cấp và lỗi trả về; dữ liệu tiền tệ giữ nguyên từ backend." />
      <form className="filter-bar filter-bar--admin" onSubmit={applyFilters}>
        <Field label="Tìm giao dịch" value={draft.query} onChange={(event) => setDraft({ ...draft, query: event.target.value })} placeholder="Mã checkout, email hoặc tham chiếu" />
        <SelectField label="Trạng thái" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}><option value="">Tất cả</option><option value="PENDING">Chờ xử lý</option><option value="AUTHORIZED">Đã ủy quyền</option><option value="SUCCEEDED">Thành công</option><option value="FAILED">Thất bại</option><option value="CANCELLED">Đã hủy</option><option value="PARTIALLY_REFUNDED">Hoàn một phần</option><option value="REFUNDED">Đã hoàn</option></SelectField>
        <SelectField label="Nhà cung cấp" value={draft.provider} onChange={(event) => setDraft({ ...draft, provider: event.target.value })}><option value="">Tất cả</option><option value="COD">COD</option><option value="MOCK_ONLINE">Mock online</option><option value="VNPAY">VNPay</option><option value="MOMO">MoMo</option><option value="ZALOPAY">ZaloPay</option><option value="STRIPE">Stripe</option></SelectField>
        <Button disabled={paymentsQuery.isFetching}><Search aria-hidden="true" /> {paymentsQuery.isFetching ? "Đang tìm…" : "Áp dụng"}</Button>
        {(Object.values(draft).some(Boolean) || Object.values(filters).some(Boolean)) ? <Button className="button-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(0); }}>Xóa lọc</Button> : null}
      </form>
      {paymentsQuery.isPending ? <LoadingPanel rows={9} label="Đang tải danh sách thanh toán" /> : paymentsQuery.isError ? <ErrorPanel error={paymentsQuery.error} onRetry={() => void paymentsQuery.refetch()} /> : payments.length === 0 ? <EmptyPanel title="Không tìm thấy giao dịch" description="Không có thanh toán phù hợp với bộ lọc hiện tại." /> : (
        <section className="panel table-panel">
          <div className="data-table admin-payment-table" role="table" aria-label="Danh sách thanh toán">
            <div className="data-row data-row--head" role="row"><span>Checkout</span><span>Khách hàng</span><span>Nhà cung cấp</span><span>Số tiền</span><span>Trạng thái</span><span>Cập nhật</span><span>Hành động</span></div>
            {payments.map((payment) => <article className="data-row" role="row" key={payment.id}>
              <div data-label="Checkout"><strong>{payment.checkoutNumber || shortId(payment.checkoutGroupId)}</strong><small>{shortId(payment.id)}</small></div>
              <div data-label="Khách hàng"><span>{payment.customerEmail || "Không có email"}</span><small>{shortId(payment.userId)}</small></div>
              <div data-label="Nhà cung cấp"><strong>{payment.provider || "—"}</strong><small className="mono">{payment.providerReference || "Chưa có tham chiếu"}</small></div>
              <span data-label="Số tiền" className="numeric strong-money">{formatMoney(payment.amount, payment.currency)}</span>
              <span data-label="Trạng thái"><StatusBadge value={payment.status} /></span>
              <span data-label="Cập nhật">{formatBusinessTime(payment.updatedAt)}</span>
              <div data-label="Hành động" className="row-actions"><Button className="button-quiet" type="button" onClick={() => setDetailId(payment.id ?? "")}><Eye aria-hidden="true" /> Chi tiết</Button></div>
            </article>)}
          </div>
          <Pagination page={page} totalPages={paymentsQuery.data?.totalPages ?? 1} onChange={setPage} disabled={paymentsQuery.isFetching} />
        </section>
      )}
      <Dialog open={Boolean(detailId)} title="Chi tiết thanh toán" description={detailId ? `ID ${shortId(detailId)}` : undefined} onClose={() => setDetailId("")}>
        <div className="dialog-body">
          {detailQuery.isPending ? <LoadingPanel rows={7} /> : detailQuery.isError ? <ErrorPanel error={detailQuery.error} onRetry={() => void detailQuery.refetch()} /> : detailQuery.data ? <PaymentDetail payment={detailQuery.data} /> : null}
        </div>
      </Dialog>
    </>
  );
}

function PaymentDetail({ payment }: { payment: AdminPaymentSummary }) {
  return (
    <>
      <div className="entity-heading"><div><strong>{payment.checkoutNumber || shortId(payment.checkoutGroupId)}</strong><span>{payment.customerEmail || shortId(payment.userId)}</span></div><StatusBadge value={payment.status} /></div>
      <dl className="detail-grid detail-grid--three"><div><dt>Số tiền</dt><dd className="strong-money">{formatMoney(payment.amount, payment.currency)}</dd></div><div><dt>Nhà cung cấp</dt><dd>{payment.provider || "—"}</dd></div><div><dt>Mã tham chiếu</dt><dd className="mono">{payment.providerReference || "—"}</dd></div><div><dt>Tạo lúc</dt><dd>{formatBusinessTime(payment.createdAt)}</dd></div><div><dt>Thanh toán lúc</dt><dd>{formatBusinessTime(payment.paidAt)}</dd></div><div><dt>Cập nhật lúc</dt><dd>{formatBusinessTime(payment.updatedAt)}</dd></div></dl>
      {payment.failureCode || payment.failureMessage ? <Notice tone="error"><strong>{payment.failureCode || "PAYMENT_FAILED"}</strong><br />{payment.failureMessage || "Nhà cung cấp không trả thông tin lỗi."}</Notice> : <Notice tone="info">Giao dịch không có lỗi nhà cung cấp được ghi nhận.</Notice>}
      <section className="dialog-section"><h3>Định danh liên kết</h3><dl className="detail-grid"><div><dt>Checkout group</dt><dd className="mono">{payment.checkoutGroupId || "—"}</dd></div><div><dt>Người dùng</dt><dd className="mono">{payment.userId || "—"}</dd></div></dl></section>
    </>
  );
}
