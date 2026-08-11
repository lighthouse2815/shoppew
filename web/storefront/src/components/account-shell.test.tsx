import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AccountShell } from "./account-shell";

const auth = vi.hoisted(() => ({ logout: vi.fn() }));
const router = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  usePathname: () => "/account/orders",
  useRouter: () => router,
}));

vi.mock("./providers", () => ({ useAuth: () => auth }));

describe("đăng xuất phiên hiện tại từ khu vực tài khoản", () => {
  beforeEach(() => {
    auth.logout.mockReset();
    router.replace.mockReset();
  });

  it("chặn gửi lặp trong lúc chờ và điều hướng đến đăng nhập khi thành công", async () => {
    let finishLogout: (() => void) | undefined;
    auth.logout.mockImplementation(() => new Promise<void>((resolve) => {
      finishLogout = resolve;
    }));

    render(<AccountShell><p>Nội dung tài khoản</p></AccountShell>);

    fireEvent.click(screen.getByRole("button", { name: "Đăng xuất phiên này" }));

    const pendingButton = screen.getByRole("button", { name: "Đang đăng xuất..." });
    expect(pendingButton).toBeDisabled();
    expect(pendingButton).toHaveAttribute("aria-busy", "true");
    fireEvent.click(pendingButton);
    expect(auth.logout).toHaveBeenCalledOnce();

    await act(async () => finishLogout?.());
    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/login"));
  });

  it("giữ người dùng tại chỗ và công bố lỗi khi máy chủ chưa xác nhận đăng xuất", async () => {
    auth.logout.mockRejectedValue(new Error("Mất kết nối"));

    render(<AccountShell><p>Nội dung tài khoản</p></AccountShell>);
    fireEvent.click(screen.getByRole("button", { name: "Đăng xuất phiên này" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Phiên hiện tại vẫn được giữ");
    expect(router.replace).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Đăng xuất phiên này" })).toHaveAttribute("aria-describedby", alert.id);
    expect(screen.getByRole("button", { name: "Đăng xuất phiên này" })).toBeEnabled();
  });
});
