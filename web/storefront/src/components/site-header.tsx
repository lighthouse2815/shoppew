"use client";

import Link from "next/link";
import { useState } from "react";
import { Bell, Menu, MessageCircle, ShoppingBag, UserRound, X } from "lucide-react";
import { useAuth } from "./providers";
import { SearchBox } from "./search-box";

export function SiteHeader() {
  const { status, user } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <>
      <div className="service-strip">
        <div className="shell service-strip__inner">
          <span>Mua sắm minh bạch · Thanh toán VND · Hỗ trợ đa nhà bán</span>
          <Link href="/account/orders">Theo dõi đơn hàng</Link>
        </div>
      </div>
      <header className="site-header">
        <div className="shell site-header__main">
          <button className="icon-button mobile-only" type="button" aria-label="Mở menu" onClick={() => setOpen(true)}><Menu /></button>
          <Link href="/" className="wordmark" aria-label="Trang chủ shoppew">shoppew<span>.</span></Link>
          <SearchBox />
          <nav className="header-actions" aria-label="Tiện ích tài khoản">
            <Link href="/account/notifications" aria-label="Thông báo"><Bell /></Link>
            <Link href="/account/messages" aria-label="Tin nhắn"><MessageCircle /></Link>
            <Link href="/cart" aria-label="Giỏ hàng"><ShoppingBag /></Link>
            <Link href={status === "authenticated" ? "/account" : "/login"} className="account-link">
              <UserRound /> <span>{user?.displayName ?? "Đăng nhập"}</span>
            </Link>
          </nav>
        </div>
        <nav className="shell category-nav" aria-label="Danh mục chính">
          <Link href="/search">Tất cả sản phẩm</Link>
          <Link href="/account/wishlist">Yêu thích</Link>
          <Link href="/account/orders">Đơn mua</Link>
          <a href="http://localhost:3001">Kênh người bán</a>
        </nav>
      </header>
      {open && (
        <div className="mobile-drawer" role="dialog" aria-modal="true" aria-label="Menu">
          <button className="icon-button mobile-drawer__close" type="button" onClick={() => setOpen(false)} aria-label="Đóng menu"><X /></button>
          <Link href="/" onClick={() => setOpen(false)}>Trang chủ</Link>
          <Link href="/search" onClick={() => setOpen(false)}>Sản phẩm</Link>
          <Link href="/cart" onClick={() => setOpen(false)}>Giỏ hàng</Link>
          <Link href="/account" onClick={() => setOpen(false)}>Tài khoản</Link>
        </div>
      )}
    </>
  );
}
