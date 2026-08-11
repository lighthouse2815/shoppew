export const nextOrderActions: Record<string, { action: string; label: string; next: string }> = {
  PAID: { action: "confirm", label: "Xác nhận đơn", next: "CONFIRMED" },
  CONFIRMED: { action: "process", label: "Bắt đầu xử lý", next: "PROCESSING" },
  PROCESSING: { action: "ready-to-ship", label: "Sẵn sàng giao", next: "READY_TO_SHIP" },
  READY_TO_SHIP: { action: "ship", label: "Bàn giao vận chuyển", next: "SHIPPED" },
  SHIPPED: { action: "deliver", label: "Xác nhận đã giao", next: "DELIVERED" },
};

export function orderCommandBody({ reason, reasonOverride, tracking, location }: { reason: string; reasonOverride?: string; tracking: string; location: string }) {
  return {
    reason: reasonOverride?.trim() || reason.trim() || undefined,
    trackingNumber: tracking.trim() || undefined,
    location: location.trim() || undefined,
  };
}
