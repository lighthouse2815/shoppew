import { describe, expect, it } from "vitest";
import { isPositive, isUuid, toOptionalNumber, validateDateRange } from "./forms";

describe("admin form validation", () => {
  it("validates UUID identifiers before moderation", () => {
    expect(isUuid("62291b11-a9d9-4383-b747-3d1ebc1bb6ef")).toBe(true);
    expect(isUuid("shop-123")).toBe(false);
  });

  it("rejects missing or reversed campaign periods", () => {
    expect(validateDateRange("", "")).toMatch(/đầy đủ/i);
    expect(validateDateRange("2026-08-12T10:00", "2026-08-11T10:00")).toMatch(/sau/i);
    expect(validateDateRange("2026-08-11T10:00", "2026-08-12T10:00")).toBeNull();
  });

  it("normalizes optional numeric inputs", () => {
    expect(toOptionalNumber("")).toBeUndefined();
    expect(toOptionalNumber("12")).toBe(12);
    expect(isPositive("0", true)).toBe(true);
    expect(isPositive("0")).toBe(false);
  });
});
