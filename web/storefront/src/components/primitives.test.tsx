import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Button, EmptyState, Field, Price } from "@shoppew/ui";

describe("UI primitives dùng chung", () => {
  it("nút thực thi callback và tôn trọng disabled", () => {
    const onClick = vi.fn();
    const { rerender } = render(<Button onClick={onClick}>Lưu</Button>);
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onClick).toHaveBeenCalledOnce();
    rerender(<Button onClick={onClick} disabled>Lưu</Button>);
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("field nối label và lỗi xác thực cho trình đọc màn hình", () => {
    render(<Field label="Email" name="email" error="Email không hợp lệ" />);
    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription("Email không hợp lệ");
  });

  it("empty state và giá tiền có nội dung thật", () => {
    render(<><EmptyState title="Giỏ hàng trống" description="Chưa có sản phẩm" /><Price value={199000} /></>);
    expect(screen.getByRole("heading", { name: "Giỏ hàng trống" })).toBeInTheDocument();
    expect(screen.getByText(/199[.\s]000/)).toBeInTheDocument();
  });
});
