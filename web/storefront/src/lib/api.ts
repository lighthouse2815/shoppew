import { ShoppewApiClient } from "@shoppew/api-client";
import type { Schema } from "./types";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:28080";
export const publicApi = new ShoppewApiClient(API_URL);

export function queryString(values: Record<string, string | number | undefined | null>) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
}

export type LoginRequest = Schema<"LoginRequest">;
export type RegisterRequest = Schema<"RegisterRequest">;
export type UpdateProfileRequest = Schema<"UpdateProfileRequest">;
export type AddressRequest = Schema<"AddressRequest">;
export type CartItemRequest = Schema<"CartItemRequest">;
export type CheckoutRequest = Schema<"CheckoutRequest">;
export type ReviewRequest = Schema<"ReviewRequest">;
