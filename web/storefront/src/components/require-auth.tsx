"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Spinner } from "@shoppew/ui";
import { useAuth } from "./providers";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (status === "anonymous") router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`);
  }, [pathname, router, status]);

  if (status === "loading") return <div className="page-center"><Spinner label="Đang kiểm tra phiên đăng nhập" /></div>;
  if (status === "anonymous") {
    return (
      <div className="page-center stack">
        <p>Bạn cần đăng nhập để tiếp tục.</p>
        <Link className="sp-button" href={`/login?returnTo=${encodeURIComponent(pathname)}`}>Đến trang đăng nhập</Link>
      </div>
    );
  }
  return children;
}
