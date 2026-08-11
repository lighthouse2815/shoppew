import type { InventoryTransaction } from "@/lib/types";

export function availableDelta(transaction: InventoryTransaction) {
  return (transaction.availableAfter ?? 0) - (transaction.availableBefore ?? 0);
}

export function signedQuantity(value: number) {
  return value > 0 ? `+${value}` : value.toString();
}
