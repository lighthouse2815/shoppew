import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { LoadingPanel } from "@/components/common";
import { ProtectedLayout } from "@/components/shell";
import { isFullAdmin } from "@/lib/access";
import { useAuth } from "@/providers";

const AuditPage = lazy(() => import("@/pages/audit").then(({ AuditPage }) => ({ default: AuditPage })));
const CampaignsPage = lazy(() => import("@/pages/campaigns").then(({ CampaignsPage }) => ({ default: CampaignsPage })));
const CatalogPage = lazy(() => import("@/pages/catalog").then(({ CatalogPage }) => ({ default: CatalogPage })));
const DashboardPage = lazy(() => import("@/pages/dashboard").then(({ DashboardPage }) => ({ default: DashboardPage })));
const DisputesPage = lazy(() => import("@/pages/disputes").then(({ DisputesPage }) => ({ default: DisputesPage })));
const LoginPage = lazy(() => import("@/pages/login").then(({ LoginPage }) => ({ default: LoginPage })));
const OrdersPage = lazy(() => import("@/pages/orders").then(({ OrdersPage }) => ({ default: OrdersPage })));
const PaymentsPage = lazy(() => import("@/pages/payments").then(({ PaymentsPage }) => ({ default: PaymentsPage })));
const ProductsPage = lazy(() => import("@/pages/products").then(({ ProductsPage }) => ({ default: ProductsPage })));
const RefundsPage = lazy(() => import("@/pages/refunds").then(({ RefundsPage }) => ({ default: RefundsPage })));
const ReviewsPage = lazy(() => import("@/pages/reviews").then(({ ReviewsPage }) => ({ default: ReviewsPage })));
const SellersPage = lazy(() => import("@/pages/sellers").then(({ SellersPage }) => ({ default: SellersPage })));
const SettingsPage = lazy(() => import("@/pages/settings").then(({ SettingsPage }) => ({ default: SettingsPage })));
const ShopsPage = lazy(() => import("@/pages/shops").then(({ ShopsPage }) => ({ default: ShopsPage })));
const UsersPage = lazy(() => import("@/pages/users").then(({ UsersPage }) => ({ default: UsersPage })));

function route(content: React.ReactNode) {
  return <Suspense fallback={<LoadingPanel rows={3} label="Đang tải trang quản trị" />}>{content}</Suspense>;
}

function FullAdminRoute({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  return isFullAdmin(user) ? children : <Navigate to="/products" replace />;
}

function HomeRoute() {
  const { user } = useAuth();
  return isFullAdmin(user) ? <DashboardPage /> : <Navigate to="/products" replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={route(<LoginPage />)} />
      <Route element={<ProtectedLayout />}>
        <Route index element={route(<HomeRoute />)} />
        <Route path="dashboard" element={route(<HomeRoute />)} />
        <Route path="users" element={route(<FullAdminRoute><UsersPage /></FullAdminRoute>)} />
        <Route path="sellers" element={route(<FullAdminRoute><SellersPage /></FullAdminRoute>)} />
        <Route path="shops" element={route(<FullAdminRoute><ShopsPage /></FullAdminRoute>)} />
        <Route path="products" element={route(<ProductsPage />)} />
        <Route path="orders" element={route(<FullAdminRoute><OrdersPage /></FullAdminRoute>)} />
        <Route path="payments" element={route(<FullAdminRoute><PaymentsPage /></FullAdminRoute>)} />
        <Route path="catalog" element={route(<FullAdminRoute><CatalogPage /></FullAdminRoute>)} />
        <Route path="categories" element={route(<FullAdminRoute><CatalogPage initialTab="categories" /></FullAdminRoute>)} />
        <Route path="brands" element={route(<FullAdminRoute><CatalogPage initialTab="brands" /></FullAdminRoute>)} />
        <Route path="campaigns" element={route(<FullAdminRoute><CampaignsPage /></FullAdminRoute>)} />
        <Route path="vouchers" element={route(<FullAdminRoute><CampaignsPage initialTab="vouchers" /></FullAdminRoute>)} />
        <Route path="promotions" element={route(<FullAdminRoute><CampaignsPage initialTab="promotions" /></FullAdminRoute>)} />
        <Route path="refunds" element={route(<FullAdminRoute><RefundsPage /></FullAdminRoute>)} />
        <Route path="disputes" element={route(<FullAdminRoute><DisputesPage /></FullAdminRoute>)} />
        <Route path="reviews" element={route(<ReviewsPage />)} />
        <Route path="audit" element={route(<FullAdminRoute><AuditPage /></FullAdminRoute>)} />
        <Route path="audit-logs" element={route(<FullAdminRoute><AuditPage /></FullAdminRoute>)} />
        <Route path="settings" element={route(<FullAdminRoute><SettingsPage /></FullAdminRoute>)} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
