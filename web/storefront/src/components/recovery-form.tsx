"use client";

import { useState } from "react";
import { Button, Field } from "@shoppew/ui";
import { ShoppewApiError } from "@shoppew/api-client";
import { useAuth } from "./providers";

export function ForgotPasswordForm() {
  const { request } = useAuth();
  const [email, setEmail] = useState("");
  const [state, setState] = useState<"idle" | "pending" | "success">("idle");
  const [error, setError] = useState("");
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(""); setState("pending");
    try { await request("/api/v1/auth/forgot-password", { method: "POST", body: { email } }); setState("success"); }
    catch (cause) { setError(cause instanceof ShoppewApiError ? cause.message : "Không thể gửi yêu cầu."); setState("idle"); }
  };
  if (state === "success") return <div className="notice notice--success">Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi. Hãy kiểm tra cả thư rác.</div>;
  return <form className="auth-form" method="post" onSubmit={submit}><Field label="Email tài khoản" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} />{error && <p className="notice notice--error">{error}</p>}<Button type="submit" disabled={state === "pending"}>{state === "pending" ? "Đang gửi..." : "Gửi hướng dẫn"}</Button></form>;
}

export function ResetPasswordForm({ token }: { token: string }) {
  const { request } = useAuth();
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [state, setState] = useState<"idle" | "pending" | "success">("idle");
  const [error, setError] = useState("");
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError("");
    if (password.length < 10 || password.length > 128) return setError("Mật khẩu cần từ 10 đến 128 ký tự.");
    if (password !== confirmation) return setError("Mật khẩu xác nhận không khớp.");
    setState("pending");
    try { await request("/api/v1/auth/reset-password", { method: "POST", body: { token, newPassword: password } }); setState("success"); }
    catch (cause) { setError(cause instanceof ShoppewApiError ? cause.message : "Không thể đặt lại mật khẩu."); setState("idle"); }
  };
  if (!token) return <div className="notice notice--error">Liên kết đặt lại mật khẩu thiếu token.</div>;
  if (state === "success") return <div className="notice notice--success">Mật khẩu đã được cập nhật. Bạn có thể quay lại trang đăng nhập.</div>;
  return <form className="auth-form" method="post" onSubmit={submit}><Field label="Mật khẩu mới" type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} /><Field label="Nhập lại mật khẩu" type="password" autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />{error && <p className="notice notice--error">{error}</p>}<Button type="submit" disabled={state === "pending"}>{state === "pending" ? "Đang cập nhật..." : "Đặt lại mật khẩu"}</Button></form>;
}
