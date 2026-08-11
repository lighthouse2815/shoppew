import { describe, expect, it } from "vitest";
import { availableDelta, signedQuantity } from "@/lib/inventory";

describe("seller inventory ledger", () => {
  it("renders stock-out and reservation movements with a negative sign", () => {
    expect(availableDelta({ availableBefore: 24, availableAfter: 23 })).toBe(-1);
    expect(signedQuantity(-1)).toBe("-1");
  });

  it("renders stock-in movements with an explicit positive sign", () => {
    expect(availableDelta({ availableBefore: 23, availableAfter: 24 })).toBe(1);
    expect(signedQuantity(1)).toBe("+1");
  });
});
