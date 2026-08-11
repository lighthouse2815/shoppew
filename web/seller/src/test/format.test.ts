import { describe, expect, it } from "vitest";
import { dateTime, money } from "@/lib/format";

describe("seller formatting", () => {
  it("formats VND without unsafe floating point presentation", () => {
    expect(money(1_250_000)).toContain("1.250.000");
    expect(money(0)).toContain("0");
  });

  it("renders timestamps in the configured Ho Chi Minh business timezone", () => {
    const rendered = dateTime("2026-08-11T00:00:00Z");
    expect(rendered).toMatch(/11\/0?8\/2026/);
    expect(rendered).toContain("07:00");
  });

  it("uses a stable placeholder for a missing timestamp", () => {
    expect(dateTime()).toBe("—");
  });
});
