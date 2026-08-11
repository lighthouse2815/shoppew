import type { components } from "@shoppew/api-client";

export type Schema<Name extends keyof components["schemas"]> = components["schemas"][Name];
export type AuthResponse = Schema<"AuthResponse">;
export type AuthUser = Schema<"AuthUserResponse">;
export type Category = Schema<"CategoryTreeResponse">;
export type Brand = Schema<"BrandResponse">;
export type ProductSummary = Schema<"ProductSummaryResponse">;
export type ProductDetail = Schema<"ProductDetailResponse">;
export type Shop = Schema<"ShopResponse">;
export type Cart = Schema<"CartResponse">;
export type CartItem = Schema<"CartItemResponse">;
export type CheckoutPreview = Schema<"CheckoutPreviewResponse">;
export type CheckoutResult = Schema<"CheckoutResponse">;
export type Profile = Schema<"ProfileResponse">;
export type Address = Schema<"AddressResponse">;
export type OrderSummary = Schema<"OrderSummaryResponse">;
export type OrderDetail = Schema<"OrderDetailResponse">;
export type WishlistItem = Schema<"WishlistResponse">;
export type Review = Schema<"ReviewResponse">;
export type Notification = Schema<"NotificationResponse">;
export type Session = Schema<"SessionResponse">;
type RequiredFields<T, Keys extends keyof T> = Omit<T, Keys> & Required<Pick<T, Keys>>;
export type Conversation = RequiredFields<Schema<"ConversationResponse">, "id" | "shopId" | "shopName" | "customerId" | "customerEmail" | "status" | "createdAt" | "updatedAt">;
export type ChatMessage = Omit<RequiredFields<Schema<"MessageResponse">, "id" | "conversationId" | "senderId" | "senderEmail" | "mine" | "sentAt">, "type"> & {
  type: Schema<"SendMessageRequest">["type"];
};
export type Page<T> = {
  content?: T[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
  first?: boolean;
  last?: boolean;
};
