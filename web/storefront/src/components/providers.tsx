"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { ShoppewApiClient, ShoppewApiError } from "@shoppew/api-client";
import { API_URL } from "@/lib/api";
import type { AuthResponse, AuthUser } from "@/lib/types";
import type { LoginRequest, RegisterRequest } from "@/lib/api";

type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthContextValue {
  status: AuthStatus;
  user: AuthUser | null;
  token: string | null;
  request: <T>(path: string, options?: Parameters<ShoppewApiClient["request"]>[1]) => Promise<T>;
  login: (input: LoginRequest) => Promise<void>;
  register: (input: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [auth, setAuth] = useState<AuthResponse | null>(null);
  const authRef = useRef<AuthResponse | null>(null);
  const refreshPromise = useRef<Promise<string | null> | null>(null);
  const rawClient = useMemo(() => new ShoppewApiClient(API_URL), []);

  const applyAuth = useCallback((next: AuthResponse | null) => {
    authRef.current = next;
    setAuth(next);
    setStatus(next?.accessToken && next.user ? "authenticated" : "anonymous");
  }, []);

  const refresh = useCallback(async () => {
    if (refreshPromise.current) return refreshPromise.current;
    refreshPromise.current = rawClient
      .request<AuthResponse>("/api/v1/auth/refresh", { method: "POST" }, false)
      .then((next) => {
        applyAuth(next);
        return next.accessToken ?? null;
      })
      .catch((error: unknown) => {
        if (error instanceof ShoppewApiError && [0, 401, 403].includes(error.status)) applyAuth(null);
        return null;
      })
      .finally(() => {
        refreshPromise.current = null;
      });
    return refreshPromise.current;
  }, [applyAuth, rawClient]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const request = useCallback<AuthContextValue["request"]>(
    (path, options = {}) => new ShoppewApiClient(API_URL, refresh).request(path, { ...options, token: options.token ?? authRef.current?.accessToken }),
    [refresh],
  );

  const login = useCallback(
    async (input: LoginRequest) => {
      const next = await rawClient.request<AuthResponse>("/api/v1/auth/login", { method: "POST", body: input });
      applyAuth(next);
    },
    [applyAuth, rawClient],
  );

  const register = useCallback(
    async (input: RegisterRequest) => {
      const next = await rawClient.request<AuthResponse>("/api/v1/auth/register", { method: "POST", body: input });
      applyAuth(next);
    },
    [applyAuth, rawClient],
  );

  const logout = useCallback(async () => {
    await request<Record<string, string>>("/api/v1/auth/logout", { method: "POST" });
    applyAuth(null);
  }, [applyAuth, request]);

  const value = useMemo(
    () => ({ status, user: auth?.user ?? null, token: auth?.accessToken ?? null, request, login, register, logout }),
    [auth, login, logout, register, request, status],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth phải được dùng bên trong Providers.");
  return context;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
          mutations: { retry: 0 },
        },
      }),
  );
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}
