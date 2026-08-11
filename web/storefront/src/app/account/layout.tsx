import { AccountShell } from "@/components/account-shell";
import { RequireAuth } from "@/components/require-auth";
import { noIndexMetadata } from "../seo";

export const metadata = noIndexMetadata(
  "Tài khoản của tôi",
  "Khu vực riêng để quản lý hồ sơ, đơn mua, địa chỉ và bảo mật trên shoppew.",
);

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return <RequireAuth><AccountShell>{children}</AccountShell></RequireAuth>;
}
