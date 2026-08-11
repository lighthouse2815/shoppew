import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Eye, Search, ShieldAlert, UserRoundCheck } from "lucide-react";
import { useState } from "react";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, SelectField, StatusBadge, TextAreaField } from "@/components/common";
import { userStatusRestriction } from "@/lib/access";
import { buildAdminQuery, displayIdentity } from "@/lib/admin";
import { apiErrorMessage, formatBusinessTime, formatNumber, shortId } from "@/lib/format";
import type { AdminUserDetail, AdminUserPage, AdminUserSummary, ManagedUserStatus } from "@/lib/types";
import { useAuth } from "@/providers";

const PAGE_SIZE = 20;

interface UserFilters {
  query: string;
  status: string;
  role: string;
}

const emptyFilters: UserFilters = { query: "", status: "", role: "" };

export function UsersPage() {
  const { request, user: operator } = useAuth();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState("");
  const [statusTarget, setStatusTarget] = useState<AdminUserSummary | null>(null);
  const [nextStatus, setNextStatus] = useState<ManagedUserStatus>("SUSPENDED");
  const [reason, setReason] = useState("");
  const [notice, setNotice] = useState("");
  const [formError, setFormError] = useState("");

  const usersQuery = useQuery({
    queryKey: ["admin-users", filters, page],
    queryFn: () => request<AdminUserPage>(`/api/v1/admin/users${buildAdminQuery({ ...filters, page, size: PAGE_SIZE })}`),
  });
  const detailQuery = useQuery({
    queryKey: ["admin-user", detailId],
    queryFn: () => request<AdminUserDetail>(`/api/v1/admin/users/${detailId}`),
    enabled: Boolean(detailId),
  });
  const statusMutation = useMutation({
    mutationFn: () => request<AdminUserDetail>(`/api/v1/admin/users/${statusTarget?.id}/status`, {
      method: "PATCH",
      body: { status: nextStatus, reason: reason.trim() },
    }),
    onSuccess: (user) => {
      setNotice(`Đã chuyển ${displayIdentity(user.displayName, user.email)} sang trạng thái ${user.status ?? nextStatus}.`);
      setStatusTarget(null);
      setReason("");
      setFormError("");
      void queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-user"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-sellers"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
    },
  });

  const users = usersQuery.data?.content ?? [];

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setPage(0);
    setFilters({ query: draft.query.trim(), status: draft.status, role: draft.role });
  }

  function openStatus(user: AdminUserSummary) {
    if (userStatusRestriction(operator, user)) return;
    setDetailId("");
    setStatusTarget(user);
    setNextStatus(user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE");
    setReason("");
    setFormError("");
    statusMutation.reset();
  }

  function submitStatus(event: React.FormEvent) {
    event.preventDefault();
    if (reason.trim().length < 3) {
      setFormError("Nêu lý do ít nhất 3 ký tự để quyết định có thể được kiểm toán.");
      return;
    }
    setFormError("");
    statusMutation.mutate();
  }

  return (
    <>
      <PageHeader eyebrow="Identity operations" title="Người dùng" description="Tra cứu tài khoản, vai trò, trạng thái xác minh và kiểm soát quyền truy cập bằng quyết định có lý do." />
      {notice ? <Notice>{notice}</Notice> : null}
      <form className="filter-bar filter-bar--admin" onSubmit={applyFilters}>
        <Field label="Tìm người dùng" value={draft.query} onChange={(event) => setDraft({ ...draft, query: event.target.value })} placeholder="Email, số điện thoại hoặc tên" />
        <SelectField label="Trạng thái" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}>
          <option value="">Tất cả</option><option value="PENDING_VERIFICATION">Chờ xác minh</option><option value="ACTIVE">Đang hoạt động</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Đã cấm</option>
        </SelectField>
        <SelectField label="Vai trò" value={draft.role} onChange={(event) => setDraft({ ...draft, role: event.target.value })}>
          <option value="">Tất cả</option><option value="CUSTOMER">Khách hàng</option><option value="SELLER">Người bán</option><option value="MODERATOR">Điều phối viên</option><option value="ADMIN">Quản trị viên</option><option value="SUPER_ADMIN">Quản trị cấp cao</option>
        </SelectField>
        <Button disabled={usersQuery.isFetching}><Search aria-hidden="true" /> {usersQuery.isFetching ? "Đang tìm…" : "Áp dụng"}</Button>
        {(draft.query || draft.status || draft.role || filters.query || filters.status || filters.role) ? <Button className="button-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(0); }}>Xóa lọc</Button> : null}
      </form>
      {usersQuery.isPending ? <LoadingPanel rows={9} label="Đang tải danh sách người dùng" /> : usersQuery.isError ? <ErrorPanel error={usersQuery.error} onRetry={() => void usersQuery.refetch()} /> : users.length === 0 ? <EmptyPanel title="Không tìm thấy người dùng" description="Không có tài khoản phù hợp với bộ lọc hiện tại. Thử xóa bớt điều kiện tìm kiếm." /> : (
        <section className="panel table-panel">
          <div className="data-table admin-identity-table" role="table" aria-label="Danh sách người dùng">
            <div className="data-row data-row--head" role="row"><span>Người dùng</span><span>Vai trò</span><span>Xác minh</span><span>Trạng thái</span><span>Ngày tạo</span><span>Hành động</span></div>
            {users.map((user) => {
              const restriction = userStatusRestriction(operator, user);
              return <article className="data-row" role="row" key={user.id}>
              <div data-label="Người dùng"><strong>{displayIdentity(user.displayName, user.email)}</strong><small>{user.email || user.phone || shortId(user.id)}</small></div>
              <div data-label="Vai trò" className="chip-list">{user.roles?.length ? user.roles.map((role) => <span className="role-chip" key={role}>{role}</span>) : <span>—</span>}</div>
              <span data-label="Xác minh">{user.emailVerified ? "Email đã xác minh" : "Chưa xác minh"}</span>
              <span data-label="Trạng thái"><StatusBadge value={user.status} /></span>
              <span data-label="Ngày tạo">{formatBusinessTime(user.createdAt)}</span>
              <div data-label="Hành động" className="row-actions"><Button className="button-quiet" type="button" onClick={() => setDetailId(user.id ?? "")}><Eye aria-hidden="true" /> Xem</Button><Button className="button-quiet" type="button" disabled={Boolean(restriction)} title={restriction ?? undefined} onClick={() => openStatus(user)}><ShieldAlert aria-hidden="true" /> Trạng thái</Button></div>
            </article>;
            })}
          </div>
          <Pagination page={page} totalPages={usersQuery.data?.totalPages ?? 1} onChange={setPage} disabled={usersQuery.isFetching} />
        </section>
      )}

      <Dialog open={Boolean(detailId)} title="Hồ sơ người dùng" description={detailId ? `ID ${shortId(detailId)}` : undefined} onClose={() => setDetailId("")}>
        <div className="dialog-body">
          {detailQuery.isPending ? <LoadingPanel rows={6} /> : detailQuery.isError ? <ErrorPanel error={detailQuery.error} onRetry={() => void detailQuery.refetch()} /> : detailQuery.data ? <UserDetail user={detailQuery.data} statusRestriction={userStatusRestriction(operator, detailQuery.data)} onChangeStatus={() => openStatus(detailQuery.data)} /> : null}
        </div>
      </Dialog>

      <Dialog open={Boolean(statusTarget)} title="Thay đổi trạng thái tài khoản" description={displayIdentity(statusTarget?.displayName, statusTarget?.email)} onClose={() => { if (!statusMutation.isPending) setStatusTarget(null); }}>
        <form onSubmit={submitStatus}>
          <div className="dialog-body">
            <p className="decision-copy"><UserRoundCheck aria-hidden="true" /> Backend sẽ áp dụng trạng thái mới ngay và ghi lý do vào nhật ký kiểm toán.</p>
            <SelectField label="Trạng thái mới" value={nextStatus} onChange={(event) => setNextStatus(event.target.value as ManagedUserStatus)}>
              <option value="ACTIVE">Kích hoạt</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Cấm tài khoản</option>
            </SelectField>
            <TextAreaField label="Lý do quyết định" required rows={4} minLength={3} maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} error={formError || undefined} />
            {statusMutation.isError ? <Notice tone="error">{apiErrorMessage(statusMutation.error)}</Notice> : null}
          </div>
          <footer className="dialog-actions"><Button className="button-secondary" type="button" disabled={statusMutation.isPending} onClick={() => setStatusTarget(null)}>Quay lại</Button><Button disabled={statusMutation.isPending || reason.trim().length < 3}>{statusMutation.isPending ? "Đang cập nhật…" : "Xác nhận trạng thái"}</Button></footer>
        </form>
      </Dialog>
    </>
  );
}

function UserDetail({ user, statusRestriction, onChangeStatus }: { user: AdminUserDetail; statusRestriction: string | null; onChangeStatus: () => void }) {
  return (
    <>
      <div className="entity-heading"><div><strong>{displayIdentity(user.displayName, user.email)}</strong><span>{user.email || "Không có email"}</span></div><StatusBadge value={user.status} /></div>
      <dl className="detail-grid detail-grid--three">
        <div><dt>Điện thoại</dt><dd>{user.phone || "—"}</dd></div><div><dt>Ngôn ngữ</dt><dd>{user.locale || "—"}</dd></div><div><dt>Giới tính</dt><dd>{user.gender || "—"}</dd></div>
        <div><dt>Ngày sinh</dt><dd>{user.dateOfBirth || "—"}</dd></div><div><dt>Phiên đang hoạt động</dt><dd>{formatNumber(user.activeSessionCount)}</dd></div><div><dt>Email</dt><dd>{user.emailVerified ? "Đã xác minh" : "Chưa xác minh"}</dd></div>
      </dl>
      <section className="dialog-section"><h3>Vai trò</h3><div className="chip-list">{user.roles?.length ? user.roles.map((role) => <span className="role-chip" key={role}>{role}</span>) : <span className="muted">Chưa có vai trò.</span>}</div></section>
      <section className="dialog-section"><h3>Gian hàng sở hữu ({user.shops?.length ?? 0})</h3>{user.shops?.length ? <ul className="plain-list">{user.shops.map((shop) => <li key={shop.id}><span><strong>{shop.name}</strong><small>{shop.slug || shortId(shop.id)}</small></span><StatusBadge value={shop.status} /></li>)}</ul> : <p className="muted">Tài khoản không sở hữu gian hàng.</p>}</section>
      <div className="dialog-inline-actions"><Button type="button" disabled={Boolean(statusRestriction)} title={statusRestriction ?? undefined} onClick={onChangeStatus}><ShieldAlert aria-hidden="true" /> Thay đổi trạng thái</Button>{statusRestriction ? <small className="muted">{statusRestriction}</small> : null}</div>
    </>
  );
}
