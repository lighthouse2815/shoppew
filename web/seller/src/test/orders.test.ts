import { describe, expect, it } from "vitest";
import { nextOrderActions, orderCommandBody } from "@/lib/order-actions";

describe("seller order command map", () => {
  it("follows the authorized fulfillment state machine", () => {
    expect(nextOrderActions.PAID).toEqual(expect.objectContaining({ action: "confirm", next: "CONFIRMED" }));
    expect(nextOrderActions.CONFIRMED).toEqual(expect.objectContaining({ action: "process", next: "PROCESSING" }));
    expect(nextOrderActions.PROCESSING).toEqual(expect.objectContaining({ action: "ready-to-ship", next: "READY_TO_SHIP" }));
    expect(nextOrderActions.READY_TO_SHIP).toEqual(expect.objectContaining({ action: "ship", next: "SHIPPED" }));
    expect(nextOrderActions.SHIPPED).toEqual(expect.objectContaining({ action: "deliver", next: "DELIVERED" }));
  });

  it("does not invent seller transitions for terminal states", () => {
    expect(nextOrderActions.COMPLETED).toBeUndefined();
    expect(nextOrderActions.CANCELLED).toBeUndefined();
    expect(nextOrderActions.REFUNDED).toBeUndefined();
  });

  it("sends the prompted cancellation reason immediately instead of stale component state", () => {
    expect(orderCommandBody({
      reason: "ghi chú cũ",
      reasonOverride: "  Khách yêu cầu hủy  ",
      tracking: "",
      location: "",
    })).toEqual({
      reason: "Khách yêu cầu hủy",
      trackingNumber: undefined,
      location: undefined,
    });
  });
});
