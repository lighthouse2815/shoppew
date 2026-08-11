import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Field } from "@shoppew/ui";
import { BanknoteArrowDown, Check, XCircle } from "lucide-react";
import { useRef, useState } from "react";
import { Dialog, EmptyPanel, ErrorPanel, LoadingPanel, Notice, PageHeader, Pagination, SelectField, StatusBadge, TextAreaField } from "@/components/common";
import { apiErrorMessage, formatBusinessTime, formatMoney } from "@/lib/format";
import { isPositive } from "@/lib/forms";
import type { Refund, RefundPage } from "@/lib/types";
import { useAuth } from "@/providers";

type RefundAction = "approve" | "reject" | "process";

export function RefundsPage() {
  const { request } = useAuth(); const queryClient = useQueryClient();
  const [status, setStatus] = useState(""); const [page, setPage] = useState(0); const [selection, setSelection] = useState<{ refund: Refund; action: RefundAction } | null>(null); const [amount, setAmount] = useState(""); const [note, setNote] = useState(""); const [formError, setFormError] = useState(""); const [notice, setNotice] = useState("");
  const processKeys = useRef(new Map<string, string>());
  const query = useQuery({ queryKey: ["admin-refunds", status, page], queryFn: () => request<RefundPage>(`/api/v1/admin/refunds?${new URLSearchParams({ ...(status ? { status } : {}), page: String(page), size: "20" })}`) });
  const mutation = useMutation({ mutationFn: async ({ refund, action }: { refund: Refund; action: RefundAction }) => {
    if (action === "approve") return request<Refund>(`/api/v1/admin/refunds/${refund.id}/approve`, { method: "POST", body: { approvedAmount: Number(amount), note: note.trim() || undefined } });
    if (action === "reject") return request<Refund>(`/api/v1/admin/refunds/${refund.id}/reject`, { method: "POST", body: { note: note.trim() } });
    const id = refund.id!; let key = processKeys.current.get(id); if (!key) { key = crypto.randomUUID(); processKeys.current.set(id, key); }
    return request<Refund>(`/api/v1/admin/refunds/${id}/process`, { method: "POST", headers: { "Idempotency-Key": key } });
  }, onSuccess: (_, variables) => { if (variables.refund.id) processKeys.current.delete(variables.refund.id); setNotice(variables.action === "approve" ? "Đã phê duyệt yêu cầu hoàn tiền." : variables.action === "reject" ? "Đã từ chối yêu cầu hoàn tiền." : "Đã gửi yêu cầu xử lý hoàn tiền idempotent."); setSelection(null); setAmount(""); setNote(""); setFormError(""); void queryClient.invalidateQueries({ queryKey: ["admin-refunds"] }); void queryClient.invalidateQueries({ queryKey: ["admin-analytics"] }); } });
  const refunds = query.data?.content ?? [];

  function open(refund: Refund, action: RefundAction) { setNotice(""); setSelection({ refund, action }); setAmount(action === "approve" ? String(refund.requestedAmount ?? "") : ""); setNote(""); setFormError(""); mutation.reset(); }
  function submit() { if (!selection) return; if (selection.action === "approve" && (!isPositive(amount) || Number(amount) > (selection.refund.requestedAmount ?? Number.MAX_SAFE_INTEGER))) { setFormError("Số tiền duyệt phải lớn hơn 0 và không vượt số tiền yêu cầu."); return; } if (selection.action === "reject" && !note.trim()) { setFormError("Cần ghi lý do từ chối."); return; } setFormError(""); mutation.mutate(selection); }

  return (
    <>
      <PageHeader eyebrow="Refund operations" title="Yêu cầu hoàn tiền" description="Duyệt số tiền, từ chối có lý do và kích hoạt xử lý hoàn tiền với khóa idempotency riêng cho từng yêu cầu." />
      <div className="filter-bar filter-bar--single"><SelectField label="Trạng thái" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">Tất cả</option><option value="REQUESTED">Mới yêu cầu</option><option value="UNDER_REVIEW">Đang xem xét</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Đã từ chối</option><option value="REFUNDING">Đang hoàn tiền</option><option value="REFUNDED">Đã hoàn tiền</option><option value="CANCELLED">Đã hủy</option></SelectField></div>
      {notice ? <Notice>{notice}</Notice> : null}
      {query.isPending ? <LoadingPanel rows={8} /> : query.isError ? <ErrorPanel error={query.error} onRetry={() => void query.refetch()} /> : refunds.length === 0 ? <EmptyPanel title="Không có yêu cầu hoàn tiền" description="Không có hồ sơ phù hợp với bộ lọc hiện tại." /> : <section className="panel table-panel"><div className="data-table refund-table" role="table" aria-label="Yêu cầu hoàn tiền"><div className="data-row data-row--head"><span>Yêu cầu</span><span>Đơn hàng</span><span>Lý do</span><span>Số tiền</span><span>Trạng thái</span><span>Hành động</span></div>{refunds.map((refund) => <article className="data-row" key={refund.id}><div data-label="Yêu cầu"><strong>{refund.requestNumber}</strong><small>{formatBusinessTime(refund.createdAt)}</small></div><div data-label="Đơn hàng"><strong>{refund.orderNumber}</strong><small>{refund.items?.length ?? 0} mặt hàng</small></div><span data-label="Lý do">{refund.reason}</span><span data-label="Số tiền" className="numeric">{formatMoney(refund.requestedAmount, refund.currency)}</span><span data-label="Trạng thái"><StatusBadge value={refund.status} /></span><div className="row-actions" data-label="Hành động">{["REQUESTED", "UNDER_REVIEW"].includes(refund.status ?? "") ? <><Button className="button-quiet action-approve" type="button" onClick={() => open(refund, "approve")}><Check aria-hidden="true" /> Duyệt</Button><Button className="button-quiet action-reject" type="button" onClick={() => open(refund, "reject")}><XCircle aria-hidden="true" /> Từ chối</Button></> : null}{refund.status === "APPROVED" ? <Button className="button-quiet" type="button" onClick={() => open(refund, "process")}><BanknoteArrowDown aria-hidden="true" /> Hoàn tiền</Button> : null}</div></article>)}</div><Pagination page={page} totalPages={query.data?.totalPages ?? 1} onChange={setPage} disabled={query.isFetching} /></section>}
      <Dialog open={Boolean(selection)} title={selection?.action === "approve" ? "Phê duyệt hoàn tiền" : selection?.action === "reject" ? "Từ chối hoàn tiền" : "Xử lý hoàn tiền"} description={selection?.refund.requestNumber} onClose={() => { if (!mutation.isPending) setSelection(null); }}><div className="dialog-body">{selection?.action === "approve" ? <><Field label="Số tiền được duyệt" type="number" min="1" required value={amount} onChange={(event) => setAmount(event.target.value)} hint={`Tối đa ${formatMoney(selection.refund.requestedAmount, selection.refund.currency)}`} /><TextAreaField label="Ghi chú" rows={4} value={note} onChange={(event) => setNote(event.target.value)} /></> : selection?.action === "reject" ? <TextAreaField label="Lý do từ chối" required rows={5} value={note} onChange={(event) => setNote(event.target.value)} /> : <p>Backend sẽ gửi giao dịch hoàn tiền với khóa idempotency ổn định cho lần xử lý này. Không đóng trang khi đang gửi.</p>}{formError ? <Notice tone="error">{formError}</Notice> : null}{mutation.isError ? <Notice tone="error">{apiErrorMessage(mutation.error)}</Notice> : null}</div><footer className="dialog-actions"><Button className="button-secondary" type="button" disabled={mutation.isPending} onClick={() => setSelection(null)}>Hủy</Button><Button type="button" disabled={mutation.isPending} onClick={submit}>{mutation.isPending ? "Đang xử lý…" : "Xác nhận"}</Button></footer></Dialog>
    </>
  );
}
