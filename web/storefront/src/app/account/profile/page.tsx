"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button, ErrorState, Field, Spinner } from "@shoppew/ui";
import { useAuth } from "@/components/providers";
import type { Profile } from "@/lib/types";

export default function ProfilePage() {
  const { request } = useAuth();
  const profile = useQuery({ queryKey: ["profile"], queryFn: () => request<Profile>("/api/v1/users/me/profile") });
  if (profile.isPending) return <Spinner label="Đang tải hồ sơ" />;
  if (profile.error) return <ErrorState message={profile.error.message} onRetry={() => void profile.refetch()} />;
  return <ProfileEditor profile={profile.data!} />;
}

function ProfileEditor({ profile }: { profile: Profile }) {
  const { request } = useAuth(); const queryClient = useQueryClient();
  const [form, setForm] = useState({ displayName: profile.displayName ?? "", phone: profile.phone ?? "", avatarUrl: profile.avatarUrl ?? "", dateOfBirth: profile.dateOfBirth ?? "", gender: profile.gender ?? "UNDISCLOSED", locale: profile.locale ?? "vi-VN" });
  const save = useMutation({ mutationFn: () => request<Profile>("/api/v1/users/me/profile", { method: "PUT", body: { ...form, phone: form.phone || undefined, avatarUrl: form.avatarUrl || undefined, dateOfBirth: form.dateOfBirth || undefined } }), onSuccess: (data) => queryClient.setQueryData(["profile"], data) });
  return <section><div className="section-heading"><div><span className="eyebrow">Thông tin cá nhân</span><h1>Hồ sơ</h1><p>Email: {profile.email}</p></div></div><form className="surface form-grid" method="post" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}><Field label="Tên hiển thị" required value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /><Field label="Số điện thoại" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /><Field label="Ngày sinh" type="date" max={new Date().toISOString().slice(0, 10)} value={form.dateOfBirth} onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })} /><label className="sp-field"><span className="sp-field__label">Giới tính</span><select className="form-control" value={form.gender} onChange={(event) => setForm({ ...form, gender: event.target.value })}><option value="UNDISCLOSED">Không muốn tiết lộ</option><option value="FEMALE">Nữ</option><option value="MALE">Nam</option><option value="NON_BINARY">Phi nhị nguyên</option></select></label><Field className="full" label="URL ảnh đại diện" type="url" value={form.avatarUrl} onChange={(event) => setForm({ ...form, avatarUrl: event.target.value })} /><div className="full form-actions"><Button type="submit" disabled={save.isPending || !form.displayName.trim()}>{save.isPending ? "Đang lưu..." : "Lưu thay đổi"}</Button>{save.isSuccess && <span className="notice notice--success">Đã cập nhật hồ sơ.</span>}{save.error && <span className="notice notice--error">{save.error.message}</span>}</div></form></section>;
}
