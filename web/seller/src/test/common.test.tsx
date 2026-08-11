import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Empty, PageHeader, Status } from "@/components/common";

describe("seller shared UI", () => {
  it("exposes a clear page heading and description", () => {
    render(<PageHeader eyebrow="Stock ledger" title="Kho hàng" description="Dữ liệu thật" />);
    expect(screen.getByRole("heading", { level: 1, name: "Kho hàng" })).toBeInTheDocument();
    expect(screen.getByText("Dữ liệu thật")).toBeInTheDocument();
  });

  it("turns backend enum values into readable status labels", () => {
    render(<Status value="PENDING_APPROVAL" />);
    expect(screen.getByText("PENDING APPROVAL")).toHaveClass("badge--pending-approval");
  });

  it("renders actionable empty-state content", () => {
    render(<Empty title="Chưa có đơn" description="Đơn mới sẽ xuất hiện ở đây." action={<button>Tải lại</button>} />);
    expect(screen.getByRole("heading", { name: "Chưa có đơn" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tải lại" })).toBeEnabled();
  });
});
