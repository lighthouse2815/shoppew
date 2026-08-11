/* eslint-disable react-refresh/only-export-components -- provider hooks are the public context API */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { ShoppewApiClient, ShoppewApiError } from "@shoppew/api-client";
import type { AuthResponse, AuthUser, Shop } from "@/lib/types";

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:28080";

interface AuthValue {
  status: "loading" | "authenticated" | "anonymous";
  user: AuthUser | null;
  request: <T>(path: string, options?: Parameters<ShoppewApiClient["request"]>[1]) => Promise<T>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}
const AuthContext = createContext<AuthValue | null>(null);

function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthValue["status"]>("loading");
  const [auth, setAuth] = useState<AuthResponse | null>(null);
  const authRef = useRef<AuthResponse | null>(null);
  const refreshPromise = useRef<Promise<string | null> | null>(null);
  const raw = useMemo(() => new ShoppewApiClient(API_URL), []);
  const apply = useCallback((next: AuthResponse | null) => { authRef.current = next; setAuth(next); setStatus(next?.accessToken && next.user ? "authenticated" : "anonymous"); }, []);
  const refresh = useCallback(async () => {
    if (refreshPromise.current) return refreshPromise.current;
    refreshPromise.current = raw.request<AuthResponse>("/api/v1/auth/refresh", { method: "POST" }, false).then((next) => { apply(next); return next.accessToken ?? null; }).catch(() => { apply(null); return null; }).finally(() => { refreshPromise.current = null; });
    return refreshPromise.current;
  }, [apply, raw]);
  useEffect(() => { void refresh(); }, [refresh]);
  const request = useCallback<AuthValue["request"]>((path, options = {}) => new ShoppewApiClient(API_URL, refresh).request(path, { ...options, token: options.token ?? authRef.current?.accessToken }), [refresh]);
  const login = useCallback(async (email: string, password: string) => { const next = await raw.request<AuthResponse>("/api/v1/auth/login", { method: "POST", body: { email, password, deviceName: "shoppew Seller Center" } }); apply(next); }, [apply, raw]);
  const logout = useCallback(async () => { try { await request("/api/v1/auth/logout", { method: "POST" }); } finally { apply(null); } }, [apply, request]);
  return <AuthContext.Provider value={{ status, user: auth?.user ?? null, request, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error("AuthProvider is required"); return value; }

interface ShopValue { shops: Shop[]; shop: Shop | null; loading: boolean; error: Error | null; selectShop: (id: string) => void; refreshShops: () => Promise<unknown>; }
const ShopContext = createContext<ShopValue | null>(null);

function ShopProvider({ children }: { children: React.ReactNode }) {
  const { status, user, request } = useAuth();
  const [selectedId, setSelectedId] = useState(() => localStorage.getItem("shoppew_seller_shop") ?? "");
  const query = useQuery({ queryKey: ["seller-shops", user?.id], queryFn: () => request<Shop[]>("/api/v1/seller/shops"), enabled: status === "authenticated" && Boolean(user?.id) });
  const shops = query.data ?? [];
  const shop = shops.find((item) => item.id === selectedId) ?? shops[0] ?? null;
  const selectShop = (id: string) => { setSelectedId(id); localStorage.setItem("shoppew_seller_shop", id); };
  return <ShopContext.Provider value={{ shops, shop, loading: query.isPending && status === "authenticated", error: query.error, selectShop, refreshShops: query.refetch }}>{children}</ShopContext.Provider>;
}

export function useShop() { const value = useContext(ShopContext); if (!value) throw new Error("ShopProvider is required"); return value; }

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { staleTime: 20_000, retry: 1, refetchOnWindowFocus: false } } }));
  return <QueryClientProvider client={client}><AuthProvider><ShopProvider>{children}</ShopProvider></AuthProvider></QueryClientProvider>;
}

export { ShoppewApiError };
