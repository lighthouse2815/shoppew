"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { Button, EmptyState, ErrorState, Spinner } from "@shoppew/ui";
import { useAuth } from "@/components/providers";
import { formatDateTime } from "@/lib/format";
import type { Notification, Page } from "@/lib/types";

export default function NotificationsPage() {
  const { request } = useAuth(); const client = useQueryClient(); const refresh = () => client.invalidateQueries({ queryKey: ["notifications"] });
  const query = useQuery({ queryKey: ["notifications"], queryFn: () => request<Page<Notification>>("/api/v1/notifications?size=50") });
  const read = useMutation({ mutationFn: (id: string) => request<Notification>(`/api/v1/notifications/${id}/read`, { method: "POST" }), onSuccess: () => void refresh() });
  const readAll = useMutation({ mutationFn: () => request<Record<string, number>>("/api/v1/notifications/read-all", { method: "POST" }), onSuccess: () => void refresh() });
  if (query.isPending) return <Spinner label="Đang tải thông báo" />; if (query.error) return <ErrorState message={query.error.message} onRetry={() => void query.refetch()} />;
  return <section><div className="section-heading"><div><span className="eyebrow">Hộp tin</span><h1>Thông báo</h1><p>Cập nhật đơn hàng, thanh toán và hệ thống.</p></div><Button className="sp-button--secondary" onClick={() => readAll.mutate()} disabled={readAll.isPending || !query.data?.content?.some((item) => !item.read)}><CheckCheck /> Đánh dấu tất cả đã đọc</Button></div>{query.data?.content?.length ? <div className="notification-list">{query.data.content.map((item) => <article key={item.id} className={item.read ? "surface" : "surface unread"}><Bell /><div><div><strong>{item.title}</strong><time>{formatDateTime(item.createdAt)}</time></div><p>{item.body}</p></div>{!item.read && item.id && <button onClick={() => read.mutate(item.id!)} disabled={read.isPending}>Đánh dấu đã đọc</button>}</article>)}</div> : <EmptyState title="Chưa có thông báo" description="Các cập nhật mới của tài khoản sẽ xuất hiện tại đây." />}</section>;
}
