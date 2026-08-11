import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { Dialog, EmptyPanel, Pagination, StatusBadge, TabSet } from "./common";

describe("admin interface primitives", () => {
  it("provides Vietnamese status text in addition to color", () => {
    render(<StatusBadge value="UNDER_REVIEW" />);
    expect(screen.getByText("Đang xem xét")).toHaveClass("status-badge--under-review");
  });

  it("keeps pagination actions labelled and bounded", () => {
    const onChange = vi.fn();
    render(<Pagination page={0} totalPages={3} onChange={onChange} />);
    expect(screen.getByRole("button", { name: /trang trước/i })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: /trang sau/i }));
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("renders an actionable empty state", () => {
    render(<EmptyPanel title="Không có hồ sơ" description="Hàng đợi trống." action={<button>Làm mới</button>} />);
    expect(screen.getByRole("heading", { name: "Không có hồ sơ" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Làm mới" })).toBeEnabled();
  });

  it("gives the modal dialog an explicit accessible name and description", () => {
    const onClose = vi.fn();
    render(<Dialog open title="Xác nhận" description="Thay đổi trạng thái người dùng" onClose={onClose}><p>Nội dung quyết định</p></Dialog>);
    const dialog = screen.getByRole("dialog", { name: "Xác nhận" });
    expect(dialog).toHaveAttribute("open");
    expect(dialog).toHaveAccessibleDescription("Thay đổi trạng thái người dùng");
    fireEvent.click(screen.getByRole("button", { name: /đóng hộp thoại/i }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("links tabs to panels and uses automatic roving keyboard focus", () => {
    const tabs = [
      { value: "categories", label: "Danh mục" },
      { value: "brands", label: "Thương hiệu" },
      { value: "attributes", label: "Thuộc tính" },
    ] as const;

    function TabsHarness() {
      const [activeTab, setActiveTab] = useState<(typeof tabs)[number]["value"]>("categories");
      return (
        <TabSet
          activeTab={activeTab}
          ariaLabel="Nhóm dữ liệu catalog"
          idPrefix="test-catalog"
          onChange={setActiveTab}
          renderPanel={(tab) => <p>Nội dung {tab}</p>}
          tabs={tabs}
        />
      );
    }

    render(<TabsHarness />);
    const categories = screen.getByRole("tab", { name: "Danh mục" });
    const brands = screen.getByRole("tab", { name: "Thương hiệu" });
    const attributes = screen.getByRole("tab", { name: "Thuộc tính" });

    expect(categories).toHaveAttribute("aria-controls", "test-catalog-panel-categories");
    expect(document.getElementById("test-catalog-panel-categories")).toHaveAttribute("role", "tabpanel");
    expect(document.getElementById("test-catalog-panel-categories")).not.toHaveAttribute("hidden");
    expect(brands).toHaveAttribute("tabindex", "-1");

    fireEvent.keyDown(categories, { key: "ArrowRight" });
    expect(brands).toHaveFocus();
    expect(brands).toHaveAttribute("aria-selected", "true");
    expect(document.getElementById("test-catalog-panel-brands")).not.toHaveAttribute("hidden");
    expect(document.getElementById("test-catalog-panel-categories")).toHaveAttribute("hidden");

    fireEvent.keyDown(brands, { key: "End" });
    expect(attributes).toHaveFocus();
    fireEvent.keyDown(attributes, { key: "ArrowRight" });
    expect(categories).toHaveFocus();
  });
});
