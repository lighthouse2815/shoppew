import { describe, expect, it } from "vitest";
import { formatBusinessTime, formatMoney, localDateTimeToIso, parseIds, toDateTimeLocal } from "./format";

describe("Vietnamese commerce formatting", () => {
  it("formats VND without decimal money", () => {
    expect(formatMoney(125000)).toContain("125.000");
    expect(formatMoney(undefined)).toBe("—");
  });

  it("converts admin local time with the Ho Chi Minh offset", () => {
    expect(localDateTimeToIso("2026-08-11T09:30")).toBe("2026-08-11T02:30:00.000Z");
    expect(toDateTimeLocal("2026-08-11T02:30:00.000Z")).toBe("2026-08-11T09:30");
    expect(formatBusinessTime("not-a-date")).toBe("—");
  });

  it("deduplicates comma and whitespace separated identifiers", () => {
    expect(parseIds("alpha, beta;alpha\ngamma")).toEqual(["alpha", "beta", "gamma"]);
  });
});
