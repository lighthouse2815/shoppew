import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Eye, Search, ShieldAlert, Store } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, SelectField, StatusBadge, TextAreaField } from "@/components/common";
import { buildAdminQuery, displayIdentity } from "@/lib/admin";
import { apiErrorMessage, formatBusinessTime, formatNumber, shortId } from "@/lib/format";
import type { AdminSellerDetail, AdminSellerPage, AdminSellerSummary, ManagedUserStatus } from "@/lib/types";
import { useAuth } from "@/providers";

const emptyFilters = { query: "", status: "", shopStatus: "" };

export function SellersPage() {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState("");
  const [statusTarget, setStatusTarget] = useState<AdminSellerSummary | null>(null);
  const [nextStatus, setNextStatus] = useState<ManagedUserStatus>("SUSPENDED");
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");

  const sellersQuery = useQuery({
    queryKey: ["admin-sellers", filters, page],
    queryFn: () => request<AdminSellerPage>(`/api/v1/admin/sellers${buildAdminQuery({ ...filters, page, size: 20 })}`),
  });
  const detailQuery = useQuery({
    queryKey: ["admin-seller", detailId],
    queryFn: () => request<AdminSellerDetail>(`/api/v1/admin/sellers/${detailId}`),
    enabled: Boolean(detailId),
  });
  const statusMutation = useMutation({
    mutationFn: () => request(`/api/v1/admin/users/${statusTarget?.userId}/status`, { method: "PATCH", body: { status: nextStatus, reason: reason.trim() } }),
    onSuccess: () => {
      setNotice(`Đã cập nhật trạng thái tài khoản của ${displayIdentity(statusTarget?.displayName, statusTarget?.email)}.`);
      setStatusTarget(null); setReason(""); setFormError("");
      void queryClient.invalidateQueries({ queryKey: ["admin-sellers"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-seller"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
    },
  });
  const sellers = sellersQuery.data?.content ?? [];

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setPage(0);
    setFilters({ query: draft.query.trim(), status: draft.status, shopStatus: draft.shopStatus });
  }

  function openStatus(seller: AdminSellerSummary) {
    setDetailId("");
    setStatusTarget(seller);
    setNextStatus(seller.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE");
    setReason(""); setFormError(""); statusMutation.reset();
  }

  function submitStatus(event: React.FormEvent) {
    event.preventDefault();
    if (reason.trim().length < 3) { setFormError("Nêu lý do ít nhất 3 ký tự để quyết định có thể được kiểm toán."); return; }
    setFormError(""); statusMutation.mutate();
  }

  return (
    <>
      <PageHeader eyebrow="Merchant operations" title="Người bán" description="Theo dõi tài khoản bán hàng, số gian hàng đang vận hành và xử lý quyền truy cập tách biệt với trạng thái từng gian hàng." />
      {notice ? <Notice>{notice}</Notice> : null}
      <form className="filter-bar filter-bar--admin" onSubmit={applyFilters}>
        <Field label="Tìm người bán" value={draft.query} onChange={(event) => setDraft({ ...draft, query: event.target.value })} placeholder="Email, điện thoại hoặc tên" />
        <SelectField label="Trạng thái tài khoản" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}><option value="">Tất cả</option><option value="PENDING_VERIFICATION">Chờ xác minh</option><option value="ACTIVE">Đang hoạt động</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Đã cấm</option></SelectField>
        <SelectField label="Trạng thái gian hàng" value={draft.shopStatus} onChange={(event) => setDraft({ ...draft, shopStatus: event.target.value })}><option value="">Tất cả</option><option value="PENDING">Chờ duyệt</option><option value="ACTIVE">Đang hoạt động</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Đã cấm</option></SelectField>
        <Button disabled={sellersQuery.isFetching}><Search aria-hidden="true" /> {sellersQuery.isFetching ? "Đang tìm…" : "Áp dụng"}</Button>
        {(draft.query || draft.status || draft.shopStatus || filters.query || filters.status || filters.shopStatus) ? <Button className="button-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(0); }}>Xóa lọc</Button> : null}
      </form>
      {sellersQuery.isPending ? <LoadingPanel rows={9} label="Đang tải danh sách người bán" /> : sellersQuery.isError ? <ErrorPanel error={sellersQuery.error} onRetry={() => void sellersQuery.refetch()} /> : sellers.length === 0 ? <EmptyPanel title="Không tìm thấy người bán" description="Không có tài khoản bán hàng phù hợp với bộ lọc hiện tại." /> : (
        <section className="panel table-panel">
          <div className="data-table admin-seller-table" role="table" aria-label="Danh sách người bán">
            <div className="data-row data-row--head" role="row"><span>Người bán</span><span>Gian hàng</span><span>Xác minh</span><span>Trạng thái</span><span>Tham gia</span><span>Hành động</span></div>
            {sellers.map((seller) => <article className="data-row" role="row" key={seller.userId}>
              <div data-label="Người bán"><strong>{displayIdentity(seller.displayName, seller.email)}</strong><small>{seller.email || seller.phone || shortId(seller.userId)}</small></div>
              <div data-label="Gian hàng"><strong>{formatNumber(seller.activeShopCount)} hoạt động</strong><small>{formatNumber(seller.shopCount)} tổng cộng</small></div>
              <span data-label="Xác minh">{seller.emailVerified ? "Email đã xác minh" : "Chưa xác minh"}</span>
              <span data-label="Trạng thái"><StatusBadge value={seller.status} /></span>
              <span data-label="Tham gia">{formatBusinessTime(seller.createdAt)}</span>
              <div data-label="Hành động" className="row-actions"><Button className="button-quiet" type="button" onClick={() => setDetailId(seller.userId ?? "")}><Eye aria-hidden="true" /> Xem</Button><Button className="button-quiet" type="button" onClick={() => openStatus(seller)}><ShieldAlert aria-hidden="true" /> Trạng thái</Button></div>
            </article>)}
          </div>
          <Pagination page={page} totalPages={sellersQuery.data?.totalPages ?? 1} onChange={setPage} disabled={sellersQuery.isFetching} />
        </section>
      )}

      <Dialog open={Boolean(detailId)} title="Hồ sơ người bán" description={detailId ? `Tài khoản ${shortId(detailId)}` : undefined} onClose={() => setDetailId("")}>
        <div className="dialog-body">
          {detailQuery.isPending ? <LoadingPanel rows={7} /> : detailQuery.isError ? <ErrorPanel error={detailQuery.error} onRetry={() => void detailQuery.refetch()} /> : detailQuery.data ? <SellerDetail detail={detailQuery.data} onChangeStatus={() => { const user = detailQuery.data?.seller; if (user) openStatus({ ...user, userId: user.id }); }} /> : null}
        </div>
      </Dialog>

      <Dialog open={Boolean(statusTarget)} title="Thay đổi trạng thái người bán" description={displayIdentity(statusTarget?.displayName, statusTarget?.email)} onClose={() => { if (!statusMutation.isPending) setStatusTarget(null); }}>
        <form onSubmit={submitStatus}><div className="dialog-body"><p className="decision-copy"><ShieldAlert aria-hidden="true" /> Đình chỉ hoặc cấm tài khoản sẽ thu hồi toàn bộ phiên làm mới. Trạng thái gian hàng vẫn được quản lý riêng.</p><SelectField label="Trạng thái mới" value={nextStatus} onChange={(event) => setNextStatus(event.target.value as ManagedUserStatus)}><option value="ACTIVE">Kích hoạt</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Cấm tài khoản</option></SelectField><TextAreaField label="Lý do quyết định" required minLength={3} maxLength={500} rows={4} value={reason} onChange={(event) => setReason(event.target.value)} error={formError || undefined} />{statusMutation.isError ? <Notice tone="error">{apiErrorMessage(statusMutation.error)}</Notice> : null}</div><footer className="dialog-actions"><Button className="button-secondary" type="button" disabled={statusMutation.isPending} onClick={() => setStatusTarget(null)}>Quay lại</Button><Button disabled={statusMutation.isPending || reason.trim().length < 3}>{statusMutation.isPending ? "Đang cập nhật…" : "Xác nhận trạng thái"}</Button></footer></form>
      </Dialog>
    </>
  );
}

function SellerDetail({ detail, onChangeStatus }: { detail: AdminSellerDetail; onChangeStatus: () => void }) {
  const seller = detail.seller;
  return (
    <>
      <div className="entity-heading"><div><strong>{displayIdentity(seller?.displayName, seller?.email)}</strong><span>{seller?.email || seller?.phone || "Không có thông tin liên hệ"}</span></div><StatusBadge value={seller?.status} /></div>
      <dl className="detail-grid"><div><dt>Email</dt><dd>{seller?.emailVerified ? "Đã xác minh" : "Chưa xác minh"}</dd></div><div><dt>Phiên hoạt động</dt><dd>{formatNumber(seller?.activeSessionCount)}</dd></div></dl>
      <section className="dialog-section"><h3>Gian hàng ({detail.shops?.length ?? 0})</h3>{detail.shops?.length ? <ul className="plain-list">{detail.shops.map((shop) => <li key={shop.id}><span><strong>{shop.name}</strong><small>{shop.slug || shortId(shop.id)}</small></span><StatusBadge value={shop.status} /></li>)}</ul> : <p className="muted">Người bán chưa có gian hàng.</p>}</section>
      <div className="dialog-inline-actions"><Button type="button" onClick={onChangeStatus}><ShieldAlert aria-hidden="true" /> Trạng thái tài khoản</Button><Link className="sp-button button-secondary" to={`/shops${detail.shops?.[0]?.slug ? `?query=${encodeURIComponent(detail.shops[0].slug!)}` : ""}`}><Store aria-hidden="true" /> Quản lý gian hàng</Link></div>
    </>
  );
}
