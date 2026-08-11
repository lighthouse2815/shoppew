import { RegisterForm } from "@/components/auth-forms";
import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Tạo tài khoản",
  "Tạo tài khoản shoppew để quản lý hành trình mua sắm từ nhiều nhà bán.",
  true,
);

export default function RegisterPage() {
  return <main className="auth-page"><div className="auth-card"><span className="eyebrow">Bắt đầu với shoppew</span><h1>Tạo tài khoản</h1><p>Thông tin của bạn được dùng để giao hàng và hỗ trợ đơn mua.</p><RegisterForm /></div><aside className="auth-aside auth-aside--violet"><span>shoppew.</span><h2>Khám phá nhiều nhà bán mà không mất dấu đơn hàng.</h2><p>Giỏ hàng được tách đúng theo shop; tổng tiền luôn được tính lại trên máy chủ khi checkout.</p></aside></main>;
}
