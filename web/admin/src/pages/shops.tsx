import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { Ban, CheckCircle2, Eye, PauseCircle, Search, Store } from "lucide-react";
import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, SelectField, StatusBadge } from "@/components/common";
import { buildAdminQuery } from "@/lib/admin";
import { apiErrorMessage, formatBusinessTime, formatNumber, shortId } from "@/lib/format";
import type { Shop, ShopPage, ShopStatusRequest } from "@/lib/types";
import { useAuth } from "@/providers";

type ShopStatus = ShopStatusRequest["status"];

export function ShopsPage() {
  const { request } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const initialQuery = searchParams.get("query") ?? "";
  const [draft, setDraft] = useState({ query: initialQuery, status: "" });
  const [filters, setFilters] = useState({ query: initialQuery, status: "" });
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState("");
  const [statusTarget, setStatusTarget] = useState<Shop | null>(null);
  const [nextStatus, setNextStatus] = useState<ShopStatus>("ACTIVE");
  const [notice, setNotice] = useState("");
  const shopsQuery = useQuery({
    queryKey: ["admin-shops", filters, page],
    queryFn: () => request<ShopPage>(`/api/v1/admin/shops${buildAdminQuery({ ...filters, page, size: 20 })}`),
  });
  const detailQuery = useQuery({
    queryKey: ["admin-shop", detailId],
    queryFn: () => request<Shop>(`/api/v1/admin/shops/${detailId}`),
    enabled: Boolean(detailId),
  });
  const mutation = useMutation({
    mutationFn: () => request<Shop>(`/api/v1/admin/shops/${statusTarget?.id}/status`, { method: "PATCH", body: { status: nextStatus } satisfies ShopStatusRequest }),
    onSuccess: (shop) => {
      setNotice(`Đã cập nhật “${shop.name || shortId(shop.id)}” sang trạng thái ${shop.status || nextStatus}.`);
      setStatusTarget(null);
      void queryClient.invalidateQueries({ queryKey: ["admin-shops"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-shop"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-sellers"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-analytics"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
    },
  });
  const shops = shopsQuery.data?.content ?? [];

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setPage(0);
    setFilters({ query: draft.query.trim(), status: draft.status });
  }

  function openStatus(shop: Shop) {
    setDetailId("");
    setStatusTarget(shop);
    setNextStatus(shop.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE");
    mutation.reset();
  }

  return (
    <>
      <PageHeader eyebrow="Merchant trust" title="Gian hàng" description="Tra cứu toàn bộ gian hàng và áp dụng quyết định kiểm duyệt trên đúng đối tượng do backend cấp." />
      {notice ? <Notice>{notice}</Notice> : null}
      <form className="filter-bar" onSubmit={applyFilters}>
        <Field label="Tìm gian hàng" value={draft.query} onChange={(event) => setDraft({ ...draft, query: event.target.value })} placeholder="Tên hoặc slug gian hàng" />
        <SelectField label="Trạng thái" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}><option value="">Tất cả</option><option value="PENDING">Chờ duyệt</option><option value="ACTIVE">Đang hoạt động</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Đã cấm</option></SelectField>
        <Button disabled={shopsQuery.isFetching}><Search aria-hidden="true" /> {shopsQuery.isFetching ? "Đang tìm…" : "Áp dụng"}</Button>
        {(draft.query || draft.status || filters.query || filters.status) ? <Button className="button-secondary" type="button" onClick={() => { setDraft({ query: "", status: "" }); setFilters({ query: "", status: "" }); setPage(0); }}>Xóa lọc</Button> : null}
      </form>
      {shopsQuery.isPending ? <LoadingPanel rows={8} label="Đang tải danh sách gian hàng" /> : shopsQuery.isError ? <ErrorPanel error={shopsQuery.error} onRetry={() => void shopsQuery.refetch()} /> : shops.length === 0 ? <EmptyPanel title="Không tìm thấy gian hàng" description="Không có gian hàng phù hợp với bộ lọc hiện tại." /> : (
        <section className="panel table-panel">
          <div className="data-table admin-shop-table" role="table" aria-label="Danh sách gian hàng">
            <div className="data-row data-row--head" role="row"><span>Gian hàng</span><span>Chủ sở hữu</span><span>Đánh giá</span><span>Trạng thái</span><span>Cập nhật</span><span>Hành động</span></div>
            {shops.map((shop) => <article className="data-row" role="row" key={shop.id}>
              <div data-label="Gian hàng"><strong>{shop.name || "Gian hàng chưa đặt tên"}</strong><small>{shop.slug || shortId(shop.id)}</small></div>
              <span data-label="Chủ sở hữu" className="mono">{shortId(shop.ownerId)}</span>
              <div data-label="Đánh giá"><strong>{shop.ratingAverage == null ? "—" : shop.ratingAverage.toFixed(1)}</strong><small>{formatNumber(shop.reviewCount)} lượt</small></div>
              <span data-label="Trạng thái"><StatusBadge value={shop.status} /></span>
              <span data-label="Cập nhật">{formatBusinessTime(shop.updatedAt)}</span>
              <div data-label="Hành động" className="row-actions"><Button className="button-quiet" type="button" onClick={() => setDetailId(shop.id ?? "")}><Eye aria-hidden="true" /> Xem</Button><Button className="button-quiet" type="button" onClick={() => openStatus(shop)}>{shop.status === "ACTIVE" ? <PauseCircle aria-hidden="true" /> : <CheckCircle2 aria-hidden="true" />} Trạng thái</Button></div>
            </article>)}
          </div>
          <Pagination page={page} totalPages={shopsQuery.data?.totalPages ?? 1} onChange={setPage} disabled={shopsQuery.isFetching} />
        </section>
      )}

      <Dialog open={Boolean(detailId)} title="Chi tiết gian hàng" description={detailId ? `ID ${shortId(detailId)}` : undefined} onClose={() => setDetailId("")}>
        <div className="dialog-body">
          {detailQuery.isPending ? <LoadingPanel rows={6} /> : detailQuery.isError ? <ErrorPanel error={detailQuery.error} onRetry={() => void detailQuery.refetch()} /> : detailQuery.data ? <><div className="entity-heading"><div><strong>{detailQuery.data.name || "Gian hàng chưa đặt tên"}</strong><span>{detailQuery.data.slug || shortId(detailQuery.data.id)}</span></div><StatusBadge value={detailQuery.data.status} /></div>{detailQuery.data.description ? <p>{detailQuery.data.description}</p> : <p className="muted">Gian hàng chưa có mô tả.</p>}<dl className="detail-grid"><div><dt>Chủ sở hữu</dt><dd className="mono">{detailQuery.data.ownerId || "—"}</dd></div><div><dt>Đánh giá</dt><dd>{detailQuery.data.ratingAverage == null ? "—" : `${detailQuery.data.ratingAverage.toFixed(1)} / 5`} · {formatNumber(detailQuery.data.reviewCount)} lượt</dd></div><div><dt>Tạo lúc</dt><dd>{formatBusinessTime(detailQuery.data.createdAt)}</dd></div><div><dt>Cập nhật lúc</dt><dd>{formatBusinessTime(detailQuery.data.updatedAt)}</dd></div></dl><div className="dialog-inline-actions"><Button type="button" onClick={() => openStatus(detailQuery.data!)}><Store aria-hidden="true" /> Thay đổi trạng thái</Button></div></> : null}
        </div>
      </Dialog>

      <Dialog open={Boolean(statusTarget)} title="Xác nhận trạng thái gian hàng" description={statusTarget?.name || shortId(statusTarget?.id)} onClose={() => { if (!mutation.isPending) setStatusTarget(null); }}>
        <div className="dialog-body"><p className="decision-copy">{nextStatus === "BANNED" ? <Ban aria-hidden="true" /> : nextStatus === "ACTIVE" ? <CheckCircle2 aria-hidden="true" /> : <PauseCircle aria-hidden="true" />} Thay đổi này ảnh hưởng khả năng vận hành của gian hàng và được backend ghi nhật ký.</p><SelectField label="Trạng thái mới" value={nextStatus} onChange={(event) => setNextStatus(event.target.value as ShopStatus)}><option value="ACTIVE">Kích hoạt</option><option value="PENDING">Chờ duyệt</option><option value="SUSPENDED">Tạm đình chỉ</option><option value="BANNED">Cấm hoạt động</option></SelectField>{mutation.isError ? <Notice tone="error">{apiErrorMessage(mutation.error)}</Notice> : null}</div>
        <footer className="dialog-actions"><Button className="button-secondary" type="button" disabled={mutation.isPending} onClick={() => setStatusTarget(null)}>Quay lại</Button><Button type="button" disabled={mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? "Đang cập nhật…" : "Xác nhận cập nhật"}</Button></footer>
      </Dialog>
    </>
  );
}
