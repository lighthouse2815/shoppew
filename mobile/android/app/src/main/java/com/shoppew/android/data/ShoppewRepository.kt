package com.shoppew.android.data

import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.AddressRequest
import com.shoppew.android.core.api.ApiEnvelope
import com.shoppew.android.core.api.AuthResponse
import com.shoppew.android.core.api.AuthUser
import com.shoppew.android.core.api.Cart
import com.shoppew.android.core.api.CartItemRequest
import com.shoppew.android.core.api.CartQuantityRequest
import com.shoppew.android.core.api.CartSelectionRequest
import com.shoppew.android.core.api.Category
import com.shoppew.android.core.api.CheckoutPreview
import com.shoppew.android.core.api.CheckoutRequest
import com.shoppew.android.core.api.CheckoutResult
import com.shoppew.android.core.api.LoginRequest
import com.shoppew.android.core.api.NotificationItem
import com.shoppew.android.core.api.OrderActionRequest
import com.shoppew.android.core.api.OrderDetail
import com.shoppew.android.core.api.OrderSummary
import com.shoppew.android.core.api.PageResponse
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.core.api.Profile
import com.shoppew.android.core.api.RegisterRequest
import com.shoppew.android.core.api.Review
import com.shoppew.android.core.api.ReviewRequest
import com.shoppew.android.core.api.ReviewUpdateRequest
import com.shoppew.android.core.api.ShoppewApi
import com.shoppew.android.core.api.UpdateProfileRequest
import com.shoppew.android.core.api.WishlistEntry
import com.shoppew.android.core.network.AccessTokenStore
import com.shoppew.android.core.network.RefreshSession
import com.shoppew.android.data.local.CatalogCache
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

interface ShoppewRepository {
    val hasRefreshSession: Boolean
    suspend fun restoreSession(): Result<AuthUser>
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun register(email: String, password: String, displayName: String, phone: String?): Result<AuthUser>
    suspend fun logout()
    suspend fun categories(): Result<List<Category>>
    suspend fun products(query: String?, categoryId: String?, page: Int = 0): Result<PageResponse<ProductSummary>>
    suspend fun product(slug: String): Result<ProductDetail>
    suspend fun recentSearches(): Result<List<String>>
    suspend fun clearSearches(): Result<Unit>
    suspend fun recentlyViewed(): Result<List<ProductSummary>>
    suspend fun productReviews(productId: String): Result<PageResponse<Review>>
    suspend fun cart(): Result<Cart>
    suspend fun addCartItem(variantId: String, quantity: Long): Result<Cart>
    suspend fun updateCartItem(itemId: String, quantity: Long): Result<Cart>
    suspend fun selectCartItem(itemId: String, selected: Boolean): Result<Cart>
    suspend fun removeCartItem(itemId: String): Result<Cart>
    suspend fun previewCheckout(request: CheckoutRequest): Result<CheckoutPreview>
    suspend fun placeCheckout(request: CheckoutRequest, idempotencyKey: String = UUID.randomUUID().toString()): Result<CheckoutResult>
    suspend fun orders(): Result<PageResponse<OrderSummary>>
    suspend fun order(orderId: String): Result<OrderDetail>
    suspend fun cancelOrder(orderId: String, reason: String): Result<OrderDetail>
    suspend fun completeOrder(orderId: String): Result<OrderDetail>
    suspend fun profile(): Result<Profile>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<Profile>
    suspend fun addresses(): Result<List<Address>>
    suspend fun createAddress(request: AddressRequest): Result<Address>
    suspend fun updateAddress(addressId: String, request: AddressRequest): Result<Address>
    suspend fun setDefaultAddress(addressId: String): Result<Address>
    suspend fun deleteAddress(addressId: String): Result<Unit>
    suspend fun wishlist(): Result<List<WishlistEntry>>
    suspend fun addWishlist(productId: String): Result<WishlistEntry>
    suspend fun removeWishlist(productId: String): Result<Unit>
    suspend fun notifications(): Result<PageResponse<NotificationItem>>
    suspend fun readNotification(notificationId: String): Result<NotificationItem>
    suspend fun readAllNotifications(): Result<Unit>
    suspend fun myReviews(): Result<PageResponse<Review>>
    suspend fun createReview(orderItemId: String, rating: Int, content: String?): Result<Review>
    suspend fun updateReview(reviewId: String, rating: Int, content: String?): Result<Review>
}

class RealShoppewRepository(
    private val publicApi: ShoppewApi,
    private val api: ShoppewApi,
    private val tokens: AccessTokenStore,
    private val cookies: RefreshSession,
    private val catalogCache: CatalogCache,
) : ShoppewRepository {
    override val hasRefreshSession: Boolean get() = cookies.hasRefreshSession()

    override suspend fun restoreSession(): Result<AuthUser> = call {
        require(hasRefreshSession) { "Chưa có phiên đăng nhập" }
        val auth = publicApi.refresh().dataOrThrow()
        saveAuth(auth)
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> = call {
        saveAuth(publicApi.login(LoginRequest(email.trim(), password)).dataOrThrow())
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        phone: String?,
    ): Result<AuthUser> = call {
        saveAuth(publicApi.register(RegisterRequest(email.trim(), password, displayName.trim(), phone?.trim())).dataOrThrow())
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        tokens.clear()
        cookies.clear()
    }

    override suspend fun categories() = cachedCall(
        network = { publicApi.categories().dataOrThrow() },
        persist = catalogCache::saveCategories,
        cached = catalogCache::categories,
    )

    override suspend fun products(query: String?, categoryId: String?, page: Int): Result<PageResponse<ProductSummary>> {
        val normalizedQuery = query?.trim()?.takeIf(String::isNotEmpty)
        runCatching { normalizedQuery?.let { catalogCache.rememberSearch(it) } }
        return cachedCall(
            network = { publicApi.products(normalizedQuery, categoryId, page).dataOrThrow() },
            persist = { response -> catalogCache.saveProducts(response.content) },
            cached = { catalogCache.products(normalizedQuery, categoryId, page, PRODUCT_PAGE_SIZE) },
        )
    }

    override suspend fun product(slug: String) = cachedCall(
        network = { publicApi.product(slug).dataOrThrow() },
        persist = { product ->
            catalogCache.saveProduct(product)
            catalogCache.rememberViewed(product)
        },
        cached = {
            catalogCache.product(slug)?.also { product ->
                runCatching { catalogCache.rememberViewed(product) }
            }
        },
    )

    override suspend fun recentSearches(): Result<List<String>> = call { catalogCache.recentSearches() }
    override suspend fun clearSearches(): Result<Unit> = call { catalogCache.clearSearches() }
    override suspend fun recentlyViewed(): Result<List<ProductSummary>> = call { catalogCache.recentlyViewed() }
    override suspend fun productReviews(productId: String) = call { publicApi.productReviews(productId).dataOrThrow() }
    override suspend fun cart() = call { api.cart().dataOrThrow() }
    override suspend fun addCartItem(variantId: String, quantity: Long) =
        call { api.addCartItem(CartItemRequest(variantId, quantity)).dataOrThrow() }
    override suspend fun updateCartItem(itemId: String, quantity: Long) =
        call { api.updateCartItem(itemId, CartQuantityRequest(quantity)).dataOrThrow() }
    override suspend fun selectCartItem(itemId: String, selected: Boolean) =
        call { api.selectCartItem(itemId, CartSelectionRequest(selected)).dataOrThrow() }
    override suspend fun removeCartItem(itemId: String) = call { api.removeCartItem(itemId).dataOrThrow() }
    override suspend fun previewCheckout(request: CheckoutRequest) = call { api.previewCheckout(request).dataOrThrow() }
    override suspend fun placeCheckout(request: CheckoutRequest, idempotencyKey: String) =
        call { api.placeCheckout(idempotencyKey, request).dataOrThrow() }
    override suspend fun orders() = call { api.orders().dataOrThrow() }
    override suspend fun order(orderId: String) = call { api.order(orderId).dataOrThrow() }
    override suspend fun cancelOrder(orderId: String, reason: String) =
        call { api.cancelOrder(orderId, OrderActionRequest(reason)).dataOrThrow() }
    override suspend fun completeOrder(orderId: String) = call { api.completeOrder(orderId).dataOrThrow() }
    override suspend fun profile() = call { api.profile().dataOrThrow() }
    override suspend fun updateProfile(request: UpdateProfileRequest) = call { api.updateProfile(request).dataOrThrow() }
    override suspend fun addresses() = call { api.addresses().dataOrThrow() }
    override suspend fun createAddress(request: AddressRequest) = call { api.createAddress(request).dataOrThrow() }
    override suspend fun updateAddress(addressId: String, request: AddressRequest) =
        call { api.updateAddress(addressId, request).dataOrThrow() }
    override suspend fun setDefaultAddress(addressId: String) = call { api.setDefaultAddress(addressId).dataOrThrow() }
    override suspend fun deleteAddress(addressId: String): Result<Unit> = call {
        api.deleteAddress(addressId).dataOrThrow()
        Unit
    }
    override suspend fun wishlist() = call { api.wishlist().dataOrThrow() }
    override suspend fun addWishlist(productId: String) = call { api.addWishlist(productId).dataOrThrow() }
    override suspend fun removeWishlist(productId: String): Result<Unit> = call {
        val response = api.removeWishlist(productId)
        if (!response.isSuccessful) throw HttpException(response)
    }
    override suspend fun notifications() = call { api.notifications().dataOrThrow() }
    override suspend fun readNotification(notificationId: String) = call { api.readNotification(notificationId).dataOrThrow() }
    override suspend fun readAllNotifications(): Result<Unit> = call {
        api.readAllNotifications().dataOrThrow()
        Unit
    }
    override suspend fun myReviews() = call { api.myReviews().dataOrThrow() }
    override suspend fun createReview(orderItemId: String, rating: Int, content: String?) =
        call { api.createReview(ReviewRequest(orderItemId, rating, content?.trim()?.takeIf(String::isNotEmpty))).dataOrThrow() }
    override suspend fun updateReview(reviewId: String, rating: Int, content: String?) =
        call { api.updateReview(reviewId, ReviewUpdateRequest(rating, content?.trim()?.takeIf(String::isNotEmpty))).dataOrThrow() }

    private fun saveAuth(auth: AuthResponse): AuthUser {
        val token = auth.accessToken ?: throw ShoppewException("AUTH_TOKEN_MISSING", "Máy chủ không trả về access token")
        val user = auth.user ?: throw ShoppewException("AUTH_USER_MISSING", "Máy chủ không trả về tài khoản")
        tokens.update(token)
        return user
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (throwable: Throwable) {
        Result.failure(throwable.toShoppewException())
    }

    private suspend fun <T> cachedCall(
        network: suspend () -> T,
        persist: suspend (T) -> Unit,
        cached: suspend () -> T?,
    ): Result<T> = try {
        val value = network()
        // A cache write must never turn an authoritative API success into a failure.
        runCatching { persist(value) }
        Result.success(value)
    } catch (throwable: Throwable) {
        val mapped = throwable.toShoppewException()
        if (mapped.code == "NETWORK_ERROR") {
            runCatching { cached() }.getOrNull()?.let { return Result.success(it) }
        }
        Result.failure(mapped)
    }

    private fun <T> ApiEnvelope<T>.dataOrThrow(): T {
        if (!success || data == null) {
            throw ShoppewException(error?.code, error?.message ?: "Yêu cầu không thành công", error?.details?.associate { it.field.orEmpty() to it.message.orEmpty() }.orEmpty())
        }
        return data
    }
}

data class ShoppewException(
    val code: String? = null,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
) : Exception(message)

private fun Throwable.toShoppewException(): ShoppewException = when (this) {
    is ShoppewException -> this
    is HttpException -> {
        val raw = response()?.errorBody()?.string()
        val parsed = runCatching {
            val error = Json.parseToJsonElement(raw.orEmpty()).jsonObject["error"]?.jsonObject
            val code = error?.get("code")?.jsonPrimitive?.contentOrNull
            val message = error?.get("message")?.jsonPrimitive?.contentOrNull
            val fields = error?.get("details")?.jsonArray.orEmpty().mapNotNull { detailElement ->
                val detail = detailElement.jsonObject
                val field = detail["field"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                val fieldMessage = detail["message"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                field to fieldMessage
            }.toMap()
            ShoppewException(code, message ?: httpMessage(code()), fields)
        }.getOrNull()
        parsed ?: ShoppewException("HTTP_${code()}", httpMessage(code()))
    }
    is IOException -> ShoppewException("NETWORK_ERROR", "Không thể kết nối máy chủ. Hãy kiểm tra mạng rồi thử lại.")
    is IllegalArgumentException -> ShoppewException("VALIDATION", message ?: "Dữ liệu chưa hợp lệ")
    else -> ShoppewException("UNEXPECTED", message ?: "Đã có lỗi xảy ra")
}

private fun httpMessage(status: Int): String = when (status) {
    401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
    403 -> "Bạn không có quyền thực hiện thao tác này."
    404 -> "Không tìm thấy dữ liệu yêu cầu."
    409 -> "Dữ liệu đã thay đổi. Vui lòng tải lại và thử lại."
    422 -> "Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại."
    429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau."
    in 500..599 -> "Hệ thống đang bận. Vui lòng thử lại sau."
    else -> "Yêu cầu không thành công ($status)."
}

private const val PRODUCT_PAGE_SIZE = 24
