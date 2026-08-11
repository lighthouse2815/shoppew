import type { components } from "@shoppew/api-client";

export type AuthResponse = components["schemas"]["AuthResponse"];
export type AuthUser = components["schemas"]["AuthUserResponse"];
export type AdminAnalytics = components["schemas"]["AdminAnalyticsResponse"];
export type AuditLog = components["schemas"]["AuditLogResponse"];
export type AuditPage = components["schemas"]["PageResponseAuditLogResponse"];
export type Shop = components["schemas"]["ShopResponse"];
export type ShopStatusRequest = components["schemas"]["ShopStatusRequest"];
export type ProductSummary = components["schemas"]["ProductSummaryResponse"];
export type ProductDetail = components["schemas"]["ProductDetailResponse"];
export type ProductPage = components["schemas"]["PageResponseProductSummaryResponse"];
export type Category = components["schemas"]["CategoryResponse"];
export type CategoryRequest = components["schemas"]["CategoryRequest"];
export type Brand = components["schemas"]["BrandResponse"];
export type BrandRequest = components["schemas"]["BrandRequest"];
export type AttributeDefinition = components["schemas"]["AttributeDefinitionResponse"];
export type AttributeDefinitionRequest = components["schemas"]["AttributeDefinitionRequest"];
export type Voucher = components["schemas"]["VoucherResponse"];
export type VoucherRequest = components["schemas"]["VoucherRequest"];
export type Promotion = components["schemas"]["PromotionResponse"];
export type PromotionRequest = components["schemas"]["PromotionRequest"];
export type Refund = components["schemas"]["RefundResponse"];
export type RefundPage = components["schemas"]["PageResponseRefundResponse"];
export type Dispute = components["schemas"]["DisputeResponse"];
export type DisputePage = components["schemas"]["PageResponseDisputeResponse"];
export type DisputeUpdateRequest = components["schemas"]["DisputeUpdateRequest"];
export type DisputeMessageRequest = components["schemas"]["DisputeMessageRequest"];
export type Review = components["schemas"]["ReviewResponse"];
export type ReviewPage = components["schemas"]["PageResponseReviewResponse"];
export type OrderDetail = components["schemas"]["OrderDetailResponse"];
export type Payment = components["schemas"]["PaymentResponse"];

export interface PageResponse<T> {
  content?: T[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

export type ShopPage = PageResponse<Shop>;

export type UserStatus = "PENDING_VERIFICATION" | "ACTIVE" | "SUSPENDED" | "BANNED";
export type ManagedUserStatus = Exclude<UserStatus, "PENDING_VERIFICATION">;

export interface AdminUserSummary {
  id?: string;
  email?: string;
  phone?: string;
  displayName?: string;
  avatarUrl?: string;
  status?: UserStatus;
  emailVerified?: boolean;
  roles?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminUserDetail extends AdminUserSummary {
  dateOfBirth?: string;
  gender?: string;
  locale?: string;
  activeSessionCount?: number;
  shops?: Shop[];
}

export type AdminUserPage = PageResponse<AdminUserSummary>;

export interface AdminSellerSummary {
  userId?: string;
  email?: string;
  phone?: string;
  displayName?: string;
  status?: UserStatus;
  emailVerified?: boolean;
  shopCount?: number;
  activeShopCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminSellerDetail {
  seller?: AdminUserDetail;
  shops?: Shop[];
}

export type AdminSellerPage = PageResponse<AdminSellerSummary>;

export interface AdminOrderSummary {
  id?: string;
  orderNumber?: string;
  checkoutGroupId?: string;
  userId?: string;
  customerEmail?: string;
  shopId?: string;
  shopName?: string;
  status?: string;
  itemCount?: number;
  grandTotal?: number;
  currency?: string;
  placedAt?: string;
  updatedAt?: string;
}

export interface AdminOrderDetail {
  userId?: string;
  customerEmail?: string;
  order?: OrderDetail;
  payment?: Payment | null;
}

export type AdminOrderPage = PageResponse<AdminOrderSummary>;

export interface AdminPaymentSummary {
  id?: string;
  checkoutGroupId?: string;
  checkoutNumber?: string;
  userId?: string;
  customerEmail?: string;
  provider?: string;
  providerReference?: string;
  status?: string;
  amount?: number;
  currency?: string;
  failureCode?: string;
  failureMessage?: string;
  paidAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type AdminPaymentPage = PageResponse<AdminPaymentSummary>;

export interface AdminSettings {
  locale?: string;
  currency?: string;
  timeZone?: string;
  availablePaymentProviders?: string[];
  availableShippingProviders?: string[];
  objectStorageProvider?: string;
  maxUploadBytes?: number;
}

export type AdminRole = "MODERATOR" | "ADMIN" | "SUPER_ADMIN";
