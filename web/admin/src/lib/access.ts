import type { AdminRole, AdminUserSummary, AuthUser } from "./types";

const OPERATOR_ROLES = new Set<AdminRole>(["MODERATOR", "ADMIN", "SUPER_ADMIN"]);
const FULL_ADMIN_ROLES = new Set<AdminRole>(["ADMIN", "SUPER_ADMIN"]);

export function hasAnyRole(user: AuthUser | null, roles: ReadonlySet<AdminRole>): boolean {
  return Boolean(user?.roles?.some((role) => roles.has(role as AdminRole)));
}

export function isAdminOperator(user: AuthUser | null): boolean {
  return hasAnyRole(user, OPERATOR_ROLES);
}

export function isFullAdmin(user: AuthUser | null): boolean {
  return hasAnyRole(user, FULL_ADMIN_ROLES);
}

export function roleLabel(user: AuthUser | null): string {
  if (user?.roles?.includes("SUPER_ADMIN")) return "Quản trị cấp cao";
  if (user?.roles?.includes("ADMIN")) return "Quản trị viên";
  if (user?.roles?.includes("MODERATOR")) return "Điều phối viên";
  return "Không có quyền quản trị";
}

export function userStatusRestriction(operator: AuthUser | null, target: AdminUserSummary): string | null {
  if (operator?.id && operator.id === target.id) return "Không thể tự thay đổi trạng thái tài khoản quản trị.";
  const targetIsAdmin = target.roles?.some((role) => role === "ADMIN" || role === "SUPER_ADMIN") ?? false;
  if (targetIsAdmin && !operator?.roles?.includes("SUPER_ADMIN")) return "Chỉ quản trị cấp cao mới có thể thay đổi tài khoản quản trị.";
  return null;
}
