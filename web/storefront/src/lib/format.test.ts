import { describe, expect, it } from "vitest";
import { formatDateTime, formatMoney, orderStatusLabel } from "./format";

describe("định dạng nghiệp vụ storefront", () => {
  it("hiển thị tiền VND không dùng phần thập phân", () => {
    expect(formatMoney(407000)).toMatch(/407[.\s]000\s*₫/);
  });

  it("hiển thị thời gian theo múi giờ Việt Nam", () => {
    const formatted = formatDateTime("2026-08-10T17:00:00Z");
    expect(formatted).toContain("11");
    expect(formatted).toContain("2026");
    expect(formatted).toContain("00:00");
  });

  it("dịch trạng thái đơn phổ biến", () => {
    expect(orderStatusLabel.SHIPPED).toBe("Đang giao");
    expect(orderStatusLabel.COMPLETED).toBe("Hoàn tất");
  });
});
