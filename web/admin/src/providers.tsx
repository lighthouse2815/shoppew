/* eslint-disable react-refresh/only-export-components -- provider hooks are the public context API */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ShoppewApiClient, ShoppewApiError } from "@shoppew/api-client";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { isAdminOperator } from "@/lib/access";
import type { AuthResponse, AuthUser } from "@/lib/types";

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
  const rawClient = useMemo(() => new ShoppewApiClient(API_URL), []);

  const apply = useCallback((next: AuthResponse | null) => {
    authRef.current = next;
    setAuth(next);
    setStatus(next?.accessToken && next.user && isAdminOperator(next.user) ? "authenticated" : "anonymous");
  }, []);

  const clearUnauthorizedSession = useCallback(async () => {
    try {
      await rawClient.request("/api/v1/auth/logout", { method: "POST" }, false);
    } catch {
      // The local auth state is still cleared when the server is unavailable.
    }
    apply(null);
  }, [apply, rawClient]);

  const refresh = useCallback(async () => {
    if (refreshPromise.current) return refreshPromise.current;
    refreshPromise.current = rawClient
      .request<AuthResponse>("/api/v1/auth/refresh", { method: "POST" }, false)
      .then(async (next) => {
        if (!next.user || !isAdminOperator(next.user)) {
          await clearUnauthorizedSession();
          return null;
        }
        apply(next);
        return next.accessToken ?? null;
      })
      .catch(() => {
        apply(null);
        return null;
      })
      .finally(() => {
        refreshPromise.current = null;
      });
    return refreshPromise.current;
  }, [apply, clearUnauthorizedSession, rawClient]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const request = useCallback<AuthValue["request"]>(
    (path, options = {}) =>
      new ShoppewApiClient(API_URL, refresh).request(path, {
        ...options,
        token: options.token ?? authRef.current?.accessToken,
      }),
    [refresh],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      const next = await rawClient.request<AuthResponse>("/api/v1/auth/login", {
        method: "POST",
        body: { email, password, deviceName: "shoppew Admin" },
      });
      if (!next.user || !isAdminOperator(next.user)) {
        await clearUnauthorizedSession();
        throw new ShoppewApiError(403, {
          code: "ADMIN_ACCESS_REQUIRED",
          message: "Tài khoản chưa có quyền quản trị hoặc điều phối nội dung.",
        });
      }
      apply(next);
    },
    [apply, clearUnauthorizedSession, rawClient],
  );

  const logout = useCallback(async () => {
    try {
      await request("/api/v1/auth/logout", { method: "POST" });
    } finally {
      apply(null);
    }
  }, [apply, request]);

  return <AuthContext.Provider value={{ status, user: auth?.user ?? null, request, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider is required");
  return value;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 15_000, retry: 1, refetchOnWindowFocus: false },
          mutations: { retry: 0 },
        },
      }),
  );
  return <QueryClientProvider client={queryClient}><AuthProvider>{children}</AuthProvider></QueryClientProvider>;
}
