import {
  BadgePercent,
  ChartNoAxesCombined,
  ClipboardList,
  CreditCard,
  FileClock,
  Gavel,
  LayoutDashboard,
  LogOut,
  Menu,
  MessagesSquare,
  PackageSearch,
  RotateCcw,
  Settings,
  ShoppingBag,
  ShieldCheck,
  Tags,
  Store,
  UserRoundCog,
  UsersRound,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Navigate, NavLink, Outlet, useLocation } from "react-router-dom";
import { isAdminOperator, isFullAdmin, roleLabel } from "@/lib/access";
import { useAuth } from "@/providers";

const fullAdminItems = [
  { to: "/dashboard", label: "Tổng quan", icon: LayoutDashboard },
  { to: "/users", label: "Người dùng", icon: UsersRound },
  { to: "/sellers", label: "Người bán", icon: UserRoundCog },
  { to: "/shops", label: "Gian hàng", icon: Store },
  { to: "/products", label: "Duyệt sản phẩm", icon: PackageSearch },
  { to: "/orders", label: "Đơn hàng", icon: ShoppingBag },
  { to: "/payments", label: "Thanh toán", icon: CreditCard },
  { to: "/categories", label: "Danh mục", icon: Tags },
  { to: "/brands", label: "Thương hiệu", icon: BadgePercent },
  { to: "/vouchers", label: "Voucher", icon: BadgePercent },
  { to: "/promotions", label: "Khuyến mãi", icon: BadgePercent },
  { to: "/reviews", label: "Kiểm duyệt đánh giá", icon: MessagesSquare },
  { to: "/refunds", label: "Hoàn tiền", icon: RotateCcw },
  { to: "/disputes", label: "Tranh chấp", icon: Gavel },
  { to: "/audit-logs", label: "Nhật ký kiểm toán", icon: FileClock },
  { to: "/settings", label: "Cấu hình", icon: Settings },
];

const moderationItems = [
  { to: "/products", label: "Duyệt sản phẩm", icon: PackageSearch },
  { to: "/reviews", label: "Kiểm duyệt đánh giá", icon: MessagesSquare },
];

const focusableSelector = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
].join(",");

export function ProtectedLayout() {
  const { status, user } = useAuth();
  const location = useLocation();
  if (status === "loading") return <div className="app-loading" role="status"><span className="brand-mark">shoppew.</span><p>Đang xác minh phiên quản trị…</p></div>;
  if (status === "anonymous" || !isAdminOperator(user)) return <Navigate to="/login" replace state={{ returnTo: location.pathname }} />;
  return <AdminShell />;
}

export function AdminShell() {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const navigationLayerRef = useRef<HTMLDivElement>(null);
  const sidebarRef = useRef<HTMLElement>(null);
  const canManage = isFullAdmin(user);
  const items = canManage ? fullAdminItems : moderationItems;

  useEffect(() => {
    if (!menuOpen) return;

    const activeBeforeOpen = document.activeElement;
    const previouslyFocused = activeBeforeOpen instanceof HTMLElement && activeBeforeOpen !== document.body
      ? activeBeforeOpen
      : menuButtonRef.current;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    sidebarRef.current?.querySelector<HTMLElement>(focusableSelector)?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        setMenuOpen(false);
        return;
      }
      if (event.key !== "Tab") return;

      const layer = navigationLayerRef.current;
      if (!layer) return;
      const focusable = Array.from(layer.querySelectorAll<HTMLElement>(focusableSelector))
        .filter((element) => element.tabIndex >= 0 && element.getAttribute("aria-hidden") !== "true");
      if (!focusable.length) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === first || !layer.contains(active))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (active === last || !layer.contains(active))) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
      previouslyFocused?.focus();
    };
  }, [menuOpen]);

  return (
    <div className="admin-app">
      <div
        aria-label={menuOpen ? "Điều hướng quản trị" : undefined}
        aria-modal={menuOpen ? true : undefined}
        ref={navigationLayerRef}
        role={menuOpen ? "dialog" : undefined}
      >
        <button
          aria-controls="admin-navigation-drawer"
          aria-expanded={menuOpen}
          aria-label={menuOpen ? "Đóng điều hướng" : "Mở điều hướng"}
          className="mobile-menu"
          onClick={() => setMenuOpen((value) => !value)}
          ref={menuButtonRef}
          type="button"
        >
          {menuOpen ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
        </button>
        {menuOpen ? <button className="sidebar-scrim" type="button" tabIndex={-1} aria-label="Đóng điều hướng" onClick={() => setMenuOpen(false)} /> : null}
        <aside className={`sidebar${menuOpen ? " sidebar--open" : ""}`} id="admin-navigation-drawer" ref={sidebarRef}>
          <NavLink className="admin-brand" to={canManage ? "/dashboard" : "/products"} onClick={() => setMenuOpen(false)}>
            <span>shoppew.</span><small>Admin Operations</small>
          </NavLink>
          <div className="operator-block">
            <ShieldCheck aria-hidden="true" />
            <div><strong>{user?.displayName || user?.email}</strong><span>{roleLabel(user)}</span></div>
          </div>
          <nav aria-label="Điều hướng quản trị">
            {items.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} end={to === "/"} onClick={() => setMenuOpen(false)}>
                <Icon aria-hidden="true" /><span>{label}</span>
              </NavLink>
            ))}
          </nav>
          <button className="sidebar-logout" type="button" onClick={() => void logout()}><LogOut aria-hidden="true" /> Đăng xuất</button>
        </aside>
      </div>
      <main className="admin-main" aria-hidden={menuOpen ? true : undefined} inert={menuOpen ? true : undefined}>
        <header className="topbar">
          <div><ClipboardList aria-hidden="true" /><span>Không gian vận hành marketplace</span></div>
          <div><ChartNoAxesCombined aria-hidden="true" /><span>Dữ liệu trực tiếp từ shoppew API</span></div>
        </header>
        <div className="workspace"><Outlet /></div>
      </main>
    </div>
  );
}
