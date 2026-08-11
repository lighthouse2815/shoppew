import { Button, Field } from "@shoppew/ui";
import { LockKeyhole, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "@/lib/format";
import { useAuth } from "@/providers";

const STOREFRONT_URL = import.meta.env.VITE_STOREFRONT_URL ?? "http://localhost:3000";

export function LoginPage() {
  const { status, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  if (status === "loading") return <div className="app-loading" role="status"><span className="brand-mark">shoppew.</span><p>Đang khôi phục phiên quản trị…</p></div>;
  if (status === "authenticated") return <Navigate to="/" replace />;

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    if (!email.trim() || !password) {
      setError("Nhập đầy đủ email và mật khẩu.");
      return;
    }
    setPending(true);
    try {
      await login(email.trim(), password);
      const target = (location.state as { returnTo?: string } | null)?.returnTo ?? "/";
      navigate(target, { replace: true });
    } catch (cause) {
      setError(apiErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="admin-login">
      <section className="login-form-panel">
        <a className="admin-brand admin-brand--dark" href="/"><span>shoppew.</span><small>Admin Operations</small></a>
        <span className="login-eyebrow"><ShieldCheck aria-hidden="true" /> Truy cập có kiểm soát</span>
        <h1>Đăng nhập quản trị</h1>
        <p>Dùng tài khoản có vai trò quản trị hoặc điều phối viên nội dung. Mọi hành động quan trọng đều được backend ghi nhật ký.</p>
        <form onSubmit={submit} noValidate>
          <Field label="Email" type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          <Field label="Mật khẩu" type="password" required autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} />
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          <Button disabled={pending || !email.trim() || !password}>{pending ? "Đang xác minh…" : "Đăng nhập"}</Button>
        </form>
        <a className="text-link" href={`${STOREFRONT_URL}/forgot-password`}>Quên mật khẩu?</a>
      </section>
      <aside className="login-context">
        <LockKeyhole aria-hidden="true" />
        <span>OPERATE / REVIEW / TRACE</span>
        <h2>Một trung tâm điều hành rõ ràng cho quyết định có trách nhiệm.</h2>
        <p>Kiểm duyệt danh mục, xử lý hoàn tiền, tranh chấp và truy vết thay đổi trên cùng hợp đồng API.</p>
      </aside>
    </main>
  );
}
