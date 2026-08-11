import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AdminShell } from "./shell";

const authMocks = vi.hoisted(() => ({ logout: vi.fn() }));

vi.mock("@/providers", () => ({
  useAuth: () => ({
    logout: authMocks.logout,
    status: "authenticated",
    user: {
      displayName: "Quản trị viên",
      email: "admin@example.test",
      id: "admin-1",
      roles: ["ADMIN"],
    },
  }),
}));

describe("admin navigation drawer", () => {
  it("contains focus, closes with Escape, and restores focus to its trigger", () => {
    render(<MemoryRouter><AdminShell /></MemoryRouter>);

    const trigger = screen.getByRole("button", { name: "Mở điều hướng" });
    expect(trigger).toHaveAttribute("aria-controls", "admin-navigation-drawer");
    expect(document.getElementById("admin-navigation-drawer")).toBeInTheDocument();

    fireEvent.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("dialog", { name: "Điều hướng quản trị" })).toHaveAttribute("aria-modal", "true");
    expect(screen.getByRole("main", { hidden: true })).toHaveAttribute("aria-hidden", "true");
    expect(screen.getByRole("link", { name: /shoppew/i })).toHaveFocus();

    const logout = screen.getByRole("button", { name: "Đăng xuất" });
    logout.focus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(trigger).toHaveFocus();

    fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(logout).toHaveFocus();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("dialog", { name: "Điều hướng quản trị" })).not.toBeInTheDocument();
    expect(screen.getByRole("main")).not.toHaveAttribute("aria-hidden");
    expect(trigger).toHaveFocus();
  });
});
