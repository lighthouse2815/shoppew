import { useQuery } from "@tanstack/react-query";
import { Button } from "@shoppew/ui";
import { RefreshCw } from "lucide-react";
import { useState } from "react";
import { EmptyPanel, ErrorPanel, LoadingPanel, PageHeader, Pagination } from "@/components/common";
import { formatBusinessTime, shortId } from "@/lib/format";
import type { AuditPage } from "@/lib/types";
import { useAuth } from "@/providers";

export function AuditPage() {
  const { request } = useAuth(); const [page, setPage] = useState(0);
  const query = useQuery({ queryKey: ["admin-audit", page], queryFn: () => request<AuditPage>(`/api/v1/admin/audit-logs?page=${page}&size=20`) });
  const logs = query.data?.content ?? [];
  return (
    <>
      <PageHeader eyebrow="Accountability" title="Nhật ký kiểm toán" description="Dòng thời gian bất biến của các quyết định quản trị quan trọng, kèm tác nhân, tài nguyên và request ID." action={<Button className="button-secondary" type="button" disabled={query.isFetching} onClick={() => void query.refetch()}><RefreshCw aria-hidden="true" /> {query.isFetching ? "Đang tải…" : "Làm mới"}</Button>} />
      {query.isPending ? <LoadingPanel rows={10} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : logs.length === 0 ? <EmptyPanel title="Chưa có bản ghi" description="Các hành động quản trị được backend ghi lại sẽ xuất hiện tại đây." /> : <section className="panel audit-timeline"><ol>{logs.map((log) => <li key={log.id}><span className="audit-dot" aria-hidden="true" /><div><header><strong>{log.action || "Hành động quản trị"}</strong><time dateTime={log.createdAt}>{formatBusinessTime(log.createdAt)}</time></header><dl><div><dt>Tài nguyên</dt><dd>{log.resourceType || "—"} / {shortId(log.resourceId)}</dd></div><div><dt>Tác nhân</dt><dd>{shortId(log.actorId)}</dd></div><div><dt>Request ID</dt><dd className="mono">{log.requestId || "—"}</dd></div><div><dt>IP</dt><dd>{log.ipAddress || "—"}</dd></div></dl></div></li>)}</ol><Pagination page={page} totalPages={query.data?.totalPages ?? 1} onChange={setPage} disabled={query.isFetching} /></section>}
    </>
  );
}
