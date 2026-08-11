"use client";

import Link from "next/link";
import { Bell, Heart, Package, ShieldCheck } from "lucide-react";
import { useAuth } from "@/components/providers";

export default function AccountPage() {
  const { user } = useAuth();
  return <><div className="section-heading"><div><span className="eyebrow">Tài khoản của tôi</span><h1>Xin chào, {user?.displayName}</h1><p>Quản lý hành trình mua sắm và bảo mật tại đây.</p></div></div><div className="account-dashboard"><Link href="/account/orders"><Package /><strong>Đơn mua</strong><span>Theo dõi và xử lý đơn hàng</span></Link><Link href="/account/wishlist"><Heart /><strong>Sản phẩm yêu thích</strong><span>Quay lại các sản phẩm đã lưu</span></Link><Link href="/account/notifications"><Bell /><strong>Thông báo</strong><span>Cập nhật từ đơn hàng và hệ thống</span></Link><Link href="/account/security"><ShieldCheck /><strong>Bảo mật</strong><span>Kiểm soát các phiên đăng nhập</span></Link></div></>;
}
