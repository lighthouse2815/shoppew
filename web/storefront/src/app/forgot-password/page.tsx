import Link from "next/link";
import { ForgotPasswordForm } from "@/components/recovery-form";
import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Quên mật khẩu",
  "Yêu cầu liên kết bảo mật để khôi phục tài khoản shoppew.",
  true,
);

export default function ForgotPasswordPage() { return <main className="shell narrow-page page-section"><span className="eyebrow">Khôi phục tài khoản</span><h1>Quên mật khẩu</h1><p>Nhập email đã đăng ký để nhận liên kết bảo mật.</p><ForgotPasswordForm /><Link href="/login">← Quay lại đăng nhập</Link></main>; }
