import { describe, expect, it } from "vitest";
import { isAdminOperator, isFullAdmin, roleLabel, userStatusRestriction } from "./access";
import type { AuthUser } from "./types";

function user(roles: string[]): AuthUser {
  return { id: crypto.randomUUID(), email: "operator@shoppew.local", roles };
}

describe("admin access policy", () => {
  it("lets moderators use moderation surfaces without granting full administration", () => {
    const moderator = user(["MODERATOR"]);
    expect(isAdminOperator(moderator)).toBe(true);
    expect(isFullAdmin(moderator)).toBe(false);
    expect(roleLabel(moderator)).toBe("Điều phối viên");
  });

  it("grants full access only to admin roles", () => {
    expect(isFullAdmin(user(["ADMIN"]))).toBe(true);
    expect(isFullAdmin(user(["SUPER_ADMIN"]))).toBe(true);
    expect(isAdminOperator(user(["CUSTOMER"]))).toBe(false);
  });

  it("explains status actions blocked by admin hierarchy", () => {
    const admin = user(["ADMIN"]);
    expect(userStatusRestriction(admin, { id: admin.id, roles: ["ADMIN"] })).toContain("tự thay đổi");
    expect(userStatusRestriction(admin, { id: "target", roles: ["ADMIN"] })).toContain("quản trị cấp cao");
    expect(userStatusRestriction(user(["SUPER_ADMIN"]), { id: "target", roles: ["ADMIN"] })).toBeNull();
  });
});
