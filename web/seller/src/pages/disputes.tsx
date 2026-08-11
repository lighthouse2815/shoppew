import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MessageSquareText, Send } from "lucide-react";
import { Button } from "@shoppew/ui";
import { Empty, ErrorBlock, Loading, NeedShop, PageHeader, Status } from "@/components/common";
import { dateTime } from "@/lib/format";
import type { Dispute, Page } from "@/lib/types";
import { useAuth, useShop } from "@/providers";

export function DisputesPage() {
  const { request } = useAuth(); const { shop } = useShop(); const [selected, setSelected] = useState<string | null>(null); const id = shop?.id;
  const query = useQuery({ queryKey: ["seller-disputes", id], queryFn: () => request<Page<Dispute>>(`/api/v1/seller/shops/${id}/disputes?size=100`), enabled: Boolean(id) });
  return <NeedShop><PageHeader eyebrow="Resolution inbox" title="Tranh chấp" description="Trao đổi trong hồ sơ tập trung; mọi tin nhắn được lưu cùng vụ việc." />{query.isPending ? <Loading /> : query.error ? <ErrorBlock error={query.error} /> : query.data?.content?.length ? <div className="case-grid">{query.data.content.map((item) => <button key={item.id} className="case-card" onClick={() => item.id && setSelected(item.id)}><div><MessageSquareText /><Status value={item.status} /></div><strong>#{item.disputeNumber}</strong><span>Đơn #{item.orderNumber}</span><p>{item.reason}</p><small>{dateTime(item.updatedAt)}</small></button>)}</div> : <Empty title="Không có tranh chấp" description="Các hồ sơ cần phản hồi từ shop sẽ xuất hiện tại đây." />}{selected && <DisputeDrawer shopId={id!} disputeId={selected} close={() => setSelected(null)} />}</NeedShop>;
}

function DisputeDrawer({ shopId, disputeId, close }: { shopId: string; disputeId: string; close: () => void }) {
  const { request } = useAuth(); const queryClient = useQueryClient(); const [content, setContent] = useState("");
  const query = useQuery({ queryKey: ["seller-dispute", shopId, disputeId], queryFn: () => request<Dispute>(`/api/v1/seller/shops/${shopId}/disputes/${disputeId}`) });
  const send = useMutation({ mutationFn: () => request<Dispute>(`/api/v1/seller/shops/${shopId}/disputes/${disputeId}/messages`, { method: "POST", body: { content: content.trim(), attachments: [] } }), onSuccess: async () => { setContent(""); await query.refetch(); await queryClient.invalidateQueries({ queryKey: ["seller-disputes", shopId] }); } });
  return <div className="drawer-backdrop" onMouseDown={close}><aside className="drawer" role="dialog" aria-modal="true" aria-labelledby="dispute-title" onMouseDown={(event) => event.stopPropagation()}><div className="drawer-head"><div><span>Hồ sơ tranh chấp</span><h2 id="dispute-title">#{query.data?.disputeNumber ?? "..."}</h2><p>{query.data?.orderNumber ? `Đơn #${query.data.orderNumber}` : "Đang tải dữ liệu"}</p></div><button aria-label="Đóng" onClick={close}>×</button></div>{query.isPending ? <Loading /> : query.error ? <ErrorBlock error={query.error} /> : <div className="dispute-detail"><div className="case-summary"><Status value={query.data?.status} /><strong>{query.data?.reason}</strong><p>{query.data?.description}</p>{query.data?.resolution && <p><b>Kết luận:</b> {query.data.resolution}</p>}</div><div className="message-thread">{query.data?.messages?.length ? query.data.messages.map((message) => <article key={message.id}><span>{dateTime(message.createdAt)}</span><p>{message.content}</p></article>) : <p className="inline-empty">Chưa có tin nhắn trong hồ sơ.</p>}</div>{!["RESOLVED", "CLOSED"].includes(query.data?.status ?? "") && <div className="message-composer"><label><span>Phản hồi của shop</span><textarea rows={4} value={content} onChange={(event) => setContent(event.target.value)} /></label>{send.error && <p className="form-error">{send.error.message}</p>}<Button disabled={send.isPending || !content.trim()} onClick={() => send.mutate()}><Send /> {send.isPending ? "Đang gửi..." : "Gửi phản hồi"}</Button></div>}</div>}</aside></div>;
}
