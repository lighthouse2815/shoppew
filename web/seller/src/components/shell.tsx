import { NavLink, Navigate, Outlet, useLocation } from "react-router-dom";
import { BarChart3, Boxes, ChevronDown, CircleDollarSign, Gift, Home, LogOut, MessageCircle, MessageSquareText, Package, RotateCcw, Settings, ShoppingBag, Store, TicketPercent, Truck, Warehouse } from "lucide-react";
import { Spinner } from "@shoppew/ui";
import { useAuth, useShop } from "@/providers";

const links = [
  ["/", "Tổng quan", Home], ["/products", "Sản phẩm", Boxes], ["/inventory", "Kho hàng", Warehouse], ["/orders", "Đơn hàng", Package], ["/chat", "Tin nhắn", MessageCircle], ["/refunds", "Hoàn tiền", RotateCcw], ["/disputes", "Tranh chấp", MessageSquareText], ["/vouchers", "Voucher", Gift], ["/promotions", "Khuyến mãi", TicketPercent], ["/reviews", "Đánh giá", MessageSquareText], ["/finance", "Tài chính", CircleDollarSign], ["/analytics", "Phân tích", BarChart3], ["/addresses", "Địa chỉ shop", Truck], ["/settings", "Thiết lập", Settings],
] as const;

export function ProtectedShell() {
  const { status, user, logout } = useAuth(); const { shops, shop, selectShop, loading } = useShop(); const location = useLocation();
  if (status === "loading" || loading) return <div className="full-center"><Spinner label="Đang mở Seller Center" /></div>;
  if (status === "anonymous") return <Navigate to="/login" state={{ returnTo: location.pathname }} replace />;
  return <div className="seller-app"><aside className="sidebar"><a className="seller-brand" href="/"><span>shoppew.</span><small>Seller Center</small></a><div className="shop-switcher"><Store /><div><small>Gian hàng hiện tại</small><select value={shop?.id ?? ""} onChange={(event) => selectShop(event.target.value)} disabled={!shops.length}>{shops.length ? shops.map((item) => <option key={item.id} value={item.id}>{item.name}</option>) : <option>Chưa có shop</option>}</select></div><ChevronDown /></div><nav>{links.map(([to, label, Icon]) => <NavLink key={to} to={to} end={to === "/"}><Icon /> {label}</NavLink>)}</nav><button className="sidebar-logout" onClick={() => void logout()}><LogOut /> Đăng xuất</button></aside><div className="seller-main"><header className="topbar"><div><ShoppingBag /><span>{shop?.name ?? "Thiết lập gian hàng đầu tiên"}</span><em className={`shop-status shop-status--${shop?.status?.toLowerCase() ?? "none"}`}>{shop?.status ?? "CHƯA CÓ SHOP"}</em></div><div><span>{user?.displayName}</span><a href="http://localhost:3000">Xem storefront</a><button className="topbar-logout" aria-label="Đăng xuất" onClick={() => void logout()}><LogOut /><span>Đăng xuất</span></button></div></header><main className="workspace"><Outlet /></main></div></div>;
}
