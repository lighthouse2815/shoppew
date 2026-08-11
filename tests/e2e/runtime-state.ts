import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

export const runtimeStatePath = resolve(process.cwd(), "test-results", "e2e-runtime.json");

export interface E2eRuntimeState {
  runId: string;
  password: string;
  buyerEmail: string;
  sellerEmail: string;
  adminEmail: string;
  shopName: string;
  productName: string;
  productSlug: string;
  pendingProductName: string;
  pendingProductSlug: string;
  sourceCategoryId: string;
  sourceCategoryName: string;
  sourceAttributes: Array<{
    name: string;
    value: string;
    valueType: string;
    required: boolean;
  }>;
  pendingProductId?: string;
  lifecycleOrderId?: string;
  lifecycleOrderNumber?: string;
  lifecycleItemsSubtotal?: number;
  lifecycleGrandTotal?: number;
  lifecycleTrackingNumber?: string;
}

export function readRuntimeState(): E2eRuntimeState {
  return JSON.parse(readFileSync(runtimeStatePath, "utf8")) as E2eRuntimeState;
}

export function writeRuntimeState(state: E2eRuntimeState): void {
  writeFileSync(runtimeStatePath, JSON.stringify(state, null, 2), "utf8");
}
