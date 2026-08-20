package com.shoppew.android.core.api

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal = when (decoder) {
        is JsonDecoder -> decoder.decodeJsonElement().jsonPrimitive.content.toBigDecimal()
        else -> decoder.decodeString().toBigDecimal()
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonPrimitive(value))
        } else {
            encoder.encodeString(value.toPlainString())
        }
    }
}

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: String? = null,
)

@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val details: List<ErrorDetail> = emptyList(),
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceName: String = "shoppew Android")

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val phone: String? = null,
    val deviceName: String = "shoppew Android",
)

@Serializable
data class AuthResponse(
    val accessToken: String? = null,
    val tokenType: String? = null,
    val expiresAt: String? = null,
    val requiresEmailVerification: Boolean = false,
    val user: AuthUser? = null,
)

@Serializable
data class AuthUser(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val roles: List<String> = emptyList(),
    val status: String = "",
    val emailVerified: Boolean = false,
)

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val imageUrl: String? = null,
    val children: List<Category> = emptyList(),
)

@Serializable
data class ProductSummary(
    val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val brandId: String? = null,
    val brandName: String? = null,
    val name: String = "",
    val slug: String = "",
    val shortDescription: String? = null,
    val status: String = "",
    val primaryImageUrl: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val minimumPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val originalMinimumPrice: BigDecimal? = null,
    val currency: String = "VND",
    @Serializable(with = BigDecimalSerializer::class)
    val ratingAverage: BigDecimal? = null,
    val reviewCount: Long = 0,
    val soldCount: Long = 0,
    val publishedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class ProductDetail(
    val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val shopSlug: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val brandId: String? = null,
    val brandName: String? = null,
    val name: String = "",
    val slug: String = "",
    val shortDescription: String? = null,
    val description: String = "",
    val status: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val ratingAverage: BigDecimal? = null,
    val reviewCount: Long = 0,
    val soldCount: Long = 0,
    val images: List<ProductImage> = emptyList(),
    val options: List<ProductOption> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val attributes: List<ProductAttribute> = emptyList(),
)

@Serializable
data class ProductImage(
    val id: String = "",
    val url: String = "",
    val altText: String? = null,
    val sortOrder: Int = 0,
    val primary: Boolean = false,
)

@Serializable
data class ProductOption(
    val id: String = "",
    val name: String = "",
    val sortOrder: Int = 0,
    val values: List<OptionValue> = emptyList(),
)

@Serializable
data class OptionValue(val id: String = "", val value: String = "", val sortOrder: Int = 0)

@Serializable
data class ProductAttribute(
    val attributeId: String = "",
    val name: String = "",
    val valueType: String = "",
    val required: Boolean = false,
    val value: String = "",
)

@Serializable
data class ProductVariant(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val originalPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val compareAtPrice: BigDecimal? = null,
    val currency: String = "VND",
    val imageUrl: String? = null,
    val status: String = "",
    val selections: List<VariantSelection> = emptyList(),
)

@Serializable
data class VariantSelection(
    val optionId: String = "",
    val optionName: String = "",
    val valueId: String = "",
    val value: String = "",
)

@Serializable
data class CartItemRequest(val variantId: String, val quantity: Long = 1)

@Serializable
data class CartQuantityRequest(val quantity: Long)

@Serializable
data class CartSelectionRequest(val selected: Boolean)

@Serializable
data class CartItem(
    val id: String = "",
    val shopId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productSlug: String = "",
    val imageUrl: String? = null,
    val variantId: String = "",
    val sku: String = "",
    val variantName: String = "",
    val selections: List<VariantSelection> = emptyList(),
    val quantity: Long = 0,
    val selected: Boolean = false,
    @Serializable(with = BigDecimalSerializer::class)
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val originalUnitPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val lineTotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val availableQuantity: Long = 0,
    val stockStatus: String = "",
    val eligible: Boolean = false,
    val issues: List<String> = emptyList(),
)

@Serializable
data class CartShopGroup(
    val shopId: String = "",
    val shopName: String = "",
    val shopSlug: String = "",
    val shopLogoUrl: String? = null,
    val items: List<CartItem> = emptyList(),
    @Serializable(with = BigDecimalSerializer::class)
    val selectedSubtotal: BigDecimal = BigDecimal.ZERO,
    val eligible: Boolean = false,
)

@Serializable
data class Cart(
    val id: String = "",
    val shops: List<CartShopGroup> = emptyList(),
    val itemCount: Long = 0,
    val selectedItemCount: Long = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val selectedSubtotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val revalidatedAt: String? = null,
) {
    val selectedItems: List<CartItem> get() = shops.flatMap { it.items }.filter { it.selected }
}

@Serializable
data class CheckoutRequest(
    val cartItemIds: List<String>,
    val addressId: String,
    val paymentProvider: String,
    val shippingMethodCode: String? = null,
    val customerNote: String? = null,
    val voucherCodes: List<String> = emptyList(),
)

@Serializable
data class CheckoutPreview(
    val shops: List<CheckoutShopQuote> = emptyList(),
    @Serializable(with = BigDecimalSerializer::class)
    val itemsSubtotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val shippingTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val discountTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val paymentProvider: String = "",
    val shippingMethodCode: String? = null,
    val appliedVouchers: List<VoucherApplication> = emptyList(),
    val calculatedAt: String? = null,
)

@Serializable
data class CheckoutShopQuote(
    val shopId: String = "",
    val shopName: String = "",
    val cartItemIds: List<String> = emptyList(),
    @Serializable(with = BigDecimalSerializer::class)
    val itemsSubtotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val shippingFee: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val discountTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val estimatedDeliveryFrom: String? = null,
    val estimatedDeliveryTo: String? = null,
)

@Serializable
data class VoucherApplication(
    val voucherId: String = "",
    val code: String = "",
    val name: String = "",
    val type: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
)

@Serializable
data class CheckoutResult(
    val id: String = "",
    val checkoutNumber: String = "",
    val status: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val itemsSubtotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val shippingTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val discountTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val orders: List<OrderSummary> = emptyList(),
    val payment: Payment? = null,
    val appliedVouchers: List<VoucherApplication> = emptyList(),
)

@Serializable
data class Payment(
    val id: String = "",
    val provider: String = "",
    val providerReference: String = "",
    val status: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val action: String? = null,
)

@Serializable
data class OrderSummary(
    val id: String = "",
    val orderNumber: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val status: String = "",
    val itemCount: Long = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val placedAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class OrderDetail(
    val id: String = "",
    val orderNumber: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val status: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val itemsSubtotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val shippingTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val shopDiscountTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val platformDiscountTotal: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val customerNote: String? = null,
    val address: OrderAddress? = null,
    val items: List<OrderItem> = emptyList(),
    val history: List<OrderHistory> = emptyList(),
    val shipment: Shipment? = null,
    val placedAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class OrderAddress(
    val recipientName: String = "",
    val phone: String = "",
    val countryCode: String = "VN",
    val province: String = "",
    val district: String = "",
    val ward: String? = null,
    val addressLine: String = "",
)

@Serializable
data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val variantId: String = "",
    val productName: String = "",
    val variantName: String = "",
    val sku: String = "",
    val imageUrl: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val quantity: Long = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
)

@Serializable
data class OrderHistory(
    val fromStatus: String? = null,
    val toStatus: String = "",
    val actorType: String = "",
    val reason: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class Shipment(
    val provider: String = "",
    val methodCode: String = "",
    val methodName: String = "",
    val trackingNumber: String? = null,
    val status: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val fee: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val estimatedDeliveryFrom: String? = null,
    val estimatedDeliveryTo: String? = null,
)

@Serializable
data class OrderActionRequest(val reason: String? = null)

@Serializable
data class Profile(
    val id: String = "",
    val email: String = "",
    val phone: String? = null,
    val displayName: String = "",
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val locale: String = "vi-VN",
    val roles: List<String> = emptyList(),
    val status: String = "",
    val emailVerified: Boolean = false,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val locale: String = "vi-VN",
    val phone: String? = null,
)

@Serializable
data class AddressRequest(
    val label: String? = null,
    val recipientName: String,
    val phone: String,
    val countryCode: String = "VN",
    val province: String,
    val district: String,
    val ward: String? = null,
    val addressLine: String,
    val postalCode: String? = null,
    val defaultAddress: Boolean = false,
)

@Serializable
data class Address(
    val id: String = "",
    val label: String? = null,
    val recipientName: String = "",
    val phone: String = "",
    val countryCode: String = "VN",
    val province: String = "",
    val district: String = "",
    val ward: String? = null,
    val addressLine: String = "",
    val postalCode: String? = null,
    val defaultAddress: Boolean = false,
)

@Serializable
data class WishlistEntry(val id: String = "", val product: ProductSummary? = null, val createdAt: String? = null)

@Serializable
data class NotificationItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val readAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class UnreadCount(val count: Long = 0)

@Serializable
data class PushDeviceRequest(
    val platform: String = "ANDROID",
    val targetType: String = "FID",
    val target: String,
)

@Serializable
data class PushDeviceRevocationRequest(val target: String)

@Serializable
data class PushDevice(
    val id: String = "",
    val platform: String = "ANDROID",
    val targetType: String = "FID",
    val active: Boolean = false,
    val lastSeenAt: String? = null,
)

@Serializable
data class ReviewRequest(val orderItemId: String, val rating: Int, val content: String? = null)

@Serializable
data class ReviewUpdateRequest(val rating: Int? = null, val content: String? = null)

@Serializable
data class Review(
    val id: String = "",
    val userId: String = "",
    val reviewerName: String = "",
    val reviewerAvatarUrl: String? = null,
    val shopId: String = "",
    val productId: String = "",
    val orderItemId: String = "",
    val rating: Int = 0,
    val content: String? = null,
    val status: String = "",
    val sellerReply: String? = null,
    val sellerRepliedAt: String? = null,
    val images: List<ReviewImage> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ReviewImage(val id: String = "", val url: String = "", val sortOrder: Int = 0)
