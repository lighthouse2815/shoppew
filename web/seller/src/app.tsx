import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { Loading } from "@/components/common";
import { ProtectedShell } from "@/components/shell";

const LoginPage = lazy(() => import("@/pages/login").then(({ LoginPage }) => ({ default: LoginPage })));
const DashboardPage = lazy(() => import("@/pages/dashboard").then(({ DashboardPage }) => ({ default: DashboardPage })));
const ProductsPage = lazy(() => import("@/pages/products").then(({ ProductsPage }) => ({ default: ProductsPage })));
const ProductEditorPage = lazy(() => import("@/pages/products").then(({ ProductEditorPage }) => ({ default: ProductEditorPage })));
const InventoryPage = lazy(() => import("@/pages/inventory").then(({ InventoryPage }) => ({ default: InventoryPage })));
const OrdersPage = lazy(() => import("@/pages/orders").then(({ OrdersPage }) => ({ default: OrdersPage })));
const OrderPage = lazy(() => import("@/pages/orders").then(({ OrderPage }) => ({ default: OrderPage })));
const VouchersPage = lazy(() => import("@/pages/vouchers").then(({ VouchersPage }) => ({ default: VouchersPage })));
const PromotionsPage = lazy(() => import("@/pages/promotions").then(({ PromotionsPage }) => ({ default: PromotionsPage })));
const ReviewsPage = lazy(() => import("@/pages/reviews").then(({ ReviewsPage }) => ({ default: ReviewsPage })));
const FinancePage = lazy(() => import("@/pages/finance").then(({ FinancePage }) => ({ default: FinancePage })));
const AnalyticsPage = lazy(() => import("@/pages/analytics").then(({ AnalyticsPage }) => ({ default: AnalyticsPage })));
const SettingsPage = lazy(() => import("@/pages/settings").then(({ SettingsPage }) => ({ default: SettingsPage })));
const AddressesPage = lazy(() => import("@/pages/addresses").then(({ AddressesPage }) => ({ default: AddressesPage })));
const RefundsPage = lazy(() => import("@/pages/refunds").then(({ RefundsPage }) => ({ default: RefundsPage })));
const DisputesPage = lazy(() => import("@/pages/disputes").then(({ DisputesPage }) => ({ default: DisputesPage })));
const ChatPage = lazy(() => import("@/pages/chat").then(({ ChatPage }) => ({ default: ChatPage })));

function route(content: React.ReactNode) {
  return <Suspense fallback={<Loading label="Đang tải trang" />}>{content}</Suspense>;
}

export function App() {
  return <Routes><Route path="/login" element={route(<LoginPage />)} /><Route element={<ProtectedShell />}><Route index element={route(<DashboardPage />)} /><Route path="products" element={route(<ProductsPage />)} /><Route path="products/new" element={route(<ProductEditorPage />)} /><Route path="products/:id" element={route(<ProductEditorPage />)} /><Route path="inventory" element={route(<InventoryPage />)} /><Route path="orders" element={route(<OrdersPage />)} /><Route path="orders/:id" element={route(<OrderPage />)} /><Route path="chat" element={route(<ChatPage />)} /><Route path="vouchers" element={route(<VouchersPage />)} /><Route path="promotions" element={route(<PromotionsPage />)} /><Route path="reviews" element={route(<ReviewsPage />)} /><Route path="finance" element={route(<FinancePage />)} /><Route path="analytics" element={route(<AnalyticsPage />)} /><Route path="settings" element={route(<SettingsPage />)} /><Route path="addresses" element={route(<AddressesPage />)} /><Route path="refunds" element={route(<RefundsPage />)} /><Route path="disputes" element={route(<DisputesPage />)} /></Route></Routes>;
}
