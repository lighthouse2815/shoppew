import { LoginForm } from "@/components/auth-forms";
import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Đăng nhập",
  "Đăng nhập shoppew để quản lý đơn mua, danh sách yêu thích và thông báo.",
  true,
);

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ returnTo?: string }> }) {
  const { returnTo } = await searchParams;
  return <main className="auth-page"><div className="auth-card"><span className="eyebrow">Chào mừng trở lại</span><h1>Đăng nhập shoppew</h1><p>Quản lý đơn mua, danh sách yêu thích và thông báo trong một nơi.</p><LoginForm returnTo={returnTo} /></div><aside className="auth-aside"><span>shoppew.</span><h2>Một tài khoản cho toàn bộ hành trình mua sắm.</h2><p>Phiên đăng nhập có thể thu hồi theo từng thiết bị. Access token chỉ được giữ trong bộ nhớ trình duyệt.</p></aside></main>;
}
