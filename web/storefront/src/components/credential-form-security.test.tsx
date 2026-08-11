import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginForm, RegisterForm } from "./auth-forms";
import { ForgotPasswordForm, ResetPasswordForm } from "./recovery-form";

const auth = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  request: vi.fn(),
}));
const router = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => router }));
vi.mock("./providers", () => ({ useAuth: () => auth }));

describe("fallback an toàn cho form thông tin xác thực", () => {
  beforeEach(() => {
    auth.login.mockReset().mockResolvedValue(undefined);
    auth.register.mockReset().mockResolvedValue(undefined);
    auth.request.mockReset().mockResolvedValue(undefined);
    router.replace.mockReset();
  });

  it("xuất method POST ngay trong markup trước hydration cho mọi form xác thực", () => {
    const forms = [
      renderToStaticMarkup(<LoginForm />),
      renderToStaticMarkup(<RegisterForm />),
      renderToStaticMarkup(<ForgotPasswordForm />),
      renderToStaticMarkup(<ResetPasswordForm token="reset-token" />),
    ];

    for (const markup of forms) {
      expect(markup).toMatch(/<form[^>]*method="post"/);
      expect(markup).not.toMatch(/<form[^>]*method="get"/);
    }
  });

  it("vẫn xử lý submit bằng React và không để fallback POST thay thế luồng đăng nhập", async () => {
    render(<LoginForm returnTo="/account/orders" />);
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "customer@example.test" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: "password-for-test" } });

    const form = screen.getByRole("button", { name: "Đăng nhập" }).closest("form");
    expect(form).toHaveAttribute("method", "post");
    fireEvent.submit(form!);

    await waitFor(() => expect(auth.login).toHaveBeenCalledWith({
      email: "customer@example.test",
      password: "password-for-test",
      deviceName: "shoppew storefront",
    }));
    expect(router.replace).toHaveBeenCalledWith("/account/orders");
  });
});
