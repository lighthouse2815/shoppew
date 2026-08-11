"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { Bell, Heart, House, LockKeyhole, LogOut, MessageCircle, MessageSquareText, Package, UserRound } from "lucide-react";
import { useAuth } from "./providers";

const links = [
  ["/account", "Tổng quan", House],
  ["/account/profile", "Hồ sơ", UserRound],
  ["/account/addresses", "Địa chỉ", House],
  ["/account/orders", "Đơn mua", Package],
  ["/account/wishlist", "Yêu thích", Heart],
  ["/account/reviews", "Đánh giá", MessageSquareText],
  ["/account/notifications", "Thông báo", Bell],
  ["/account/messages", "Tin nhắn", MessageCircle],
  ["/account/security", "Bảo mật", LockKeyhole],
] as const;

export function AccountShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { logout } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  async function handleLogout() {
    setLogoutError(null);
    setIsLoggingOut(true);
    try {
      await logout();
      router.replace("/login");
    } catch {
      setLogoutError("Chưa thể đăng xuất phiên này. Phiên hiện tại vẫn được giữ; vui lòng thử lại.");
    } finally {
      setIsLoggingOut(false);
    }
  }

  return (
    <div className="shell account-layout">
      <aside className="account-nav">
        <nav className="account-nav__links" aria-label="Tài khoản">
          {links.map(([href, label, Icon]) => <Link key={href} href={href} className={pathname === href || (href !== "/account" && pathname.startsWith(href)) ? "active" : ""}><Icon aria-hidden="true" /> {label}</Link>)}
        </nav>
        <button
          type="button"
          className="account-nav__logout"
          onClick={() => void handleLogout()}
          disabled={isLoggingOut}
          aria-busy={isLoggingOut}
          aria-describedby={logoutError ? "account-logout-error" : undefined}
        >
          <LogOut aria-hidden="true" /> {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất phiên này"}
        </button>
        {logoutError ? <p id="account-logout-error" className="account-nav__logout-error" role="alert">{logoutError}</p> : null}
      </aside>
      <main className="account-content">{children}</main>
    </div>
  );
}
