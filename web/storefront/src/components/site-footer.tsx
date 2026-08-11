import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="shell site-footer__grid">
        <div><div className="wordmark wordmark--footer">shoppew<span>.</span></div><p>Marketplace đa nhà bán được xây dựng cho trải nghiệm mua sắm rõ ràng và đáng tin cậy.</p></div>
        <div><h2>Mua sắm</h2><Link href="/search">Khám phá sản phẩm</Link><Link href="/cart">Giỏ hàng</Link><Link href="/account/orders">Đơn mua</Link></div>
        <div><h2>Tài khoản</h2><Link href="/account/profile">Hồ sơ</Link><Link href="/account/security">Bảo mật</Link><Link href="/account/notifications">Thông báo</Link></div>
        <div><h2>Dành cho nhà bán</h2><a href="http://localhost:3001">Seller Center</a><p>Giờ nghiệp vụ: Asia/Ho_Chi_Minh</p></div>
      </div>
      <div className="shell site-footer__bottom">© 2026 shoppew · Giá hiển thị bằng VND</div>
    </footer>
  );
}
