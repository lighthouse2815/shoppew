"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Laptop, LogOut, ShieldCheck, Smartphone } from "lucide-react";
import { Button, EmptyState, ErrorState, Spinner } from "@shoppew/ui";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/providers";
import { formatDateTime } from "@/lib/format";
import type { Session } from "@/lib/types";

export default function SecurityPage() {
  const { request, logout } = useAuth(); const router = useRouter(); const client = useQueryClient(); const refresh = () => client.invalidateQueries({ queryKey: ["sessions"] });
  const sessions = useQuery({ queryKey: ["sessions"], queryFn: () => request<Session[]>("/api/v1/auth/sessions") });
  const revoke = useMutation({ mutationFn: (id: string) => request<void>(`/api/v1/auth/sessions/${id}`, { method: "DELETE" }), onSuccess: () => void refresh() });
  const revokeAll = useMutation({ mutationFn: () => request<Record<string, number>>("/api/v1/auth/sessions", { method: "DELETE" }), onSuccess: async () => { await logout(); router.replace("/login"); } });
  if (sessions.isPending) return <Spinner label="Đang tải phiên đăng nhập" />; if (sessions.error) return <ErrorState message={sessions.error.message} onRetry={() => void sessions.refetch()} />;
  return <section><div className="section-heading"><div><span className="eyebrow">An toàn tài khoản</span><h1>Bảo mật & thiết bị</h1><p>Thu hồi những phiên bạn không còn sử dụng.</p></div></div><div className="notice"><ShieldCheck /> Refresh session được lưu trong cookie HttpOnly; access token không được lưu bền trong trình duyệt.</div><h2 className="subheading">Phiên đang hoạt động</h2>{sessions.data?.length ? <div className="session-list">{sessions.data.map((session) => <article className="surface" key={session.id}>{session.deviceName?.toLowerCase().includes("mobile") ? <Smartphone /> : <Laptop />}<div><strong>{session.deviceName || "Thiết bị không xác định"} {session.current && <span className="status-pill">Hiện tại</span>}</strong><span>{session.userAgent || "Không có thông tin trình duyệt"}</span><time>Dùng gần nhất: {formatDateTime(session.lastUsedAt)} · Hết hạn: {formatDateTime(session.expiresAt)}</time></div>{!session.current && session.id && <button onClick={() => revoke.mutate(session.id!)} disabled={revoke.isPending}>Thu hồi</button>}</article>)}</div> : <EmptyState title="Không có phiên đăng nhập" description="Phiên hiện tại có thể vừa hết hạn; hãy đăng nhập lại." />}<div className="danger-zone surface"><div><h2>Đăng xuất khỏi mọi thiết bị</h2><p>Thu hồi toàn bộ refresh session, bao gồm phiên hiện tại.</p></div><Button className="sp-button--danger" onClick={() => confirm("Đăng xuất khỏi tất cả thiết bị?") && revokeAll.mutate()} disabled={revokeAll.isPending}><LogOut /> {revokeAll.isPending ? "Đang thu hồi..." : "Đăng xuất mọi nơi"}</Button></div></section>;
}
