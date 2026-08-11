import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { Button, Field } from "@shoppew/ui";
import { ShoppewApiError, useAuth } from "@/providers";

export function LoginPage() {
  const { status, login } = useAuth(); const navigate = useNavigate(); const location = useLocation(); const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [pending, setPending] = useState(false); const [error, setError] = useState("");
  if (status === "authenticated") return <Navigate to="/" replace />;
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setPending(true); setError(""); try { await login(email, password); const target = (location.state as { returnTo?: string } | null)?.returnTo ?? "/"; navigate(target, { replace: true }); } catch (cause) { setError(cause instanceof ShoppewApiError ? cause.message : "Không thể đăng nhập."); } finally { setPending(false); } };
  return <main className="seller-login"><section><a className="seller-brand" href="/"><span>shoppew.</span><small>Seller Center</small></a><span className="login-eyebrow">Vận hành gian hàng</span><h1>Đăng nhập Seller Center</h1><p>Dùng tài khoản shoppew có quyền sở hữu hoặc thành viên gian hàng.</p><form onSubmit={submit}><Field label="Email" type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /><Field label="Mật khẩu" type="password" required autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} />{error && <p className="form-error">{error}</p>}<Button disabled={pending || !email || !password}>{pending ? "Đang đăng nhập..." : "Đăng nhập"}</Button></form><a href="http://localhost:3000/forgot-password">Quên mật khẩu?</a></section><aside><span>SELL / GROW / CONTROL</span><h2>Một bàn điều khiển gọn cho mọi nhịp vận hành.</h2><p>Sản phẩm, tồn kho, đơn hàng và dòng tiền cùng dùng dữ liệu thật từ backend shoppew.</p></aside></main>;
}
