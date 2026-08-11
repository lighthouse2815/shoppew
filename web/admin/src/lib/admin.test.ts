import { describe, expect, it } from "vitest";
import { buildAdminQuery, bytesLabel, displayIdentity } from "./admin";

describe("admin query utilities", () => {
  it("trims filters, keeps numeric paging, and omits empty values", () => {
    expect(buildAdminQuery({ query: "  an@example.test ", status: "", page: 0, size: 20, verified: false }))
      .toBe("?query=an%40example.test&page=0&size=20&verified=false");
  });

  it("uses a stable identity fallback", () => {
    expect(displayIdentity("  Nguyễn An  ", "an@example.test")).toBe("Nguyễn An");
    expect(displayIdentity("", " an@example.test ")).toBe("an@example.test");
    expect(displayIdentity()).toBe("Tài khoản chưa đặt tên");
  });

  it("formats upload limits without inventing precision", () => {
    expect(bytesLabel(10 * 1024 * 1024)).toBe("10 MB");
    expect(bytesLabel(undefined)).toBe("—");
  });
});
