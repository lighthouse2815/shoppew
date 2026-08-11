import Link from "next/link";
import { ResetPasswordForm } from "@/components/recovery-form";
import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Đặt lại mật khẩu",
  "Đặt mật khẩu mới cho tài khoản shoppew bằng liên kết khôi phục bảo mật.",
);

export default async function ResetPasswordPage({ searchParams }: { searchParams: Promise<{ token?: string }> }) { const { token = "" } = await searchParams; return <main className="shell narrow-page page-section"><span className="eyebrow">Bảo mật tài khoản</span><h1>Đặt lại mật khẩu</h1><p>Chọn mật khẩu mới chưa từng chia sẻ với người khác.</p><ResetPasswordForm token={token} /><Link href="/login">← Quay lại đăng nhập</Link></main>; }
