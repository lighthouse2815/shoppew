package com.shoppew.android.test

import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.AddressRequest
import com.shoppew.android.core.api.AuthUser
import com.shoppew.android.core.api.Cart
import com.shoppew.android.core.api.Category
import com.shoppew.android.core.api.CheckoutPreview
import com.shoppew.android.core.api.CheckoutRequest
import com.shoppew.android.core.api.CheckoutResult
import com.shoppew.android.core.api.NotificationItem
import com.shoppew.android.core.api.OrderDetail
import com.shoppew.android.core.api.OrderSummary
import com.shoppew.android.core.api.PageResponse
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.core.api.Profile
import com.shoppew.android.core.api.Review
import com.shoppew.android.core.api.UpdateProfileRequest
import com.shoppew.android.core.api.WishlistEntry
import com.shoppew.android.data.ShoppewRepository

open class TestShoppewRepository : ShoppewRepository {
    override var hasRefreshSession: Boolean = false
    var restoreResult: Result<AuthUser> = failure("restore")
    var loginResult: Result<AuthUser> = failure("login")
    var registerResult: Result<AuthUser> = failure("register")
    var categoryResult: Result<List<Category>> = Result.success(emptyList())
    var productsResult: Result<PageResponse<ProductSummary>> = Result.success(PageResponse())
    var productResult: Result<ProductDetail> = failure("product")
    var productReviewsResult: Result<PageResponse<Review>> = Result.success(PageResponse())
    var cartResult: Result<Cart> = failure("cart")
    var addressResult: Result<List<Address>> = Result.success(emptyList())
    var previewResult: Result<CheckoutPreview> = failure("preview")
    var checkoutResult: Result<CheckoutResult> = failure("checkout")
    var ordersResult: Result<PageResponse<OrderSummary>> = Result.success(PageResponse())
    var orderResult: Result<OrderDetail> = failure("order")
    var loginCalls = 0
    var previewRequests = mutableListOf<CheckoutRequest>()
    var checkoutRequests = mutableListOf<CheckoutRequest>()

    override suspend fun restoreSession(): Result<AuthUser> = restoreResult
    override suspend fun login(email: String, password: String): Result<AuthUser> {
        loginCalls += 1
        return loginResult
    }

    override suspend fun register(email: String, password: String, displayName: String, phone: String?): Result<AuthUser> = registerResult
    override suspend fun logout() = Unit
    override suspend fun categories(): Result<List<Category>> = categoryResult
    override suspend fun products(query: String?, categoryId: String?, page: Int): Result<PageResponse<ProductSummary>> = productsResult
    override suspend fun product(slug: String): Result<ProductDetail> = productResult
    override suspend fun recentSearches(): Result<List<String>> = Result.success(emptyList())
    override suspend fun clearSearches(): Result<Unit> = Result.success(Unit)
    override suspend fun recentlyViewed(): Result<List<ProductSummary>> = Result.success(emptyList())
    override suspend fun productReviews(productId: String): Result<PageResponse<Review>> = productReviewsResult
    override suspend fun cart(): Result<Cart> = cartResult
    override suspend fun addCartItem(variantId: String, quantity: Long): Result<Cart> = cartResult
    override suspend fun updateCartItem(itemId: String, quantity: Long): Result<Cart> = cartResult
    override suspend fun selectCartItem(itemId: String, selected: Boolean): Result<Cart> = cartResult
    override suspend fun removeCartItem(itemId: String): Result<Cart> = cartResult
    override suspend fun previewCheckout(request: CheckoutRequest): Result<CheckoutPreview> {
        previewRequests += request
        return previewResult
    }

    override suspend fun placeCheckout(request: CheckoutRequest, idempotencyKey: String): Result<CheckoutResult> {
        checkoutRequests += request
        return checkoutResult
    }

    override suspend fun orders(): Result<PageResponse<OrderSummary>> = ordersResult
    override suspend fun order(orderId: String): Result<OrderDetail> = orderResult
    override suspend fun cancelOrder(orderId: String, reason: String): Result<OrderDetail> = orderResult
    override suspend fun completeOrder(orderId: String): Result<OrderDetail> = orderResult
    override suspend fun profile(): Result<Profile> = failure("profile")
    override suspend fun updateProfile(request: UpdateProfileRequest): Result<Profile> = failure("profile")
    override suspend fun addresses(): Result<List<Address>> = addressResult
    override suspend fun createAddress(request: AddressRequest): Result<Address> = failure("address")
    override suspend fun updateAddress(addressId: String, request: AddressRequest): Result<Address> = failure("address")
    override suspend fun setDefaultAddress(addressId: String): Result<Address> = failure("address")
    override suspend fun deleteAddress(addressId: String): Result<Unit> = failure("address")
    override suspend fun wishlist(): Result<List<WishlistEntry>> = Result.success(emptyList())
    override suspend fun addWishlist(productId: String): Result<WishlistEntry> = failure("wishlist")
    override suspend fun removeWishlist(productId: String): Result<Unit> = failure("wishlist")
    override suspend fun notifications(): Result<PageResponse<NotificationItem>> = Result.success(PageResponse())
    override suspend fun readNotification(notificationId: String): Result<NotificationItem> = failure("notification")
    override suspend fun readAllNotifications(): Result<Unit> = Result.success(Unit)
    override suspend fun myReviews(): Result<PageResponse<Review>> = Result.success(PageResponse())
    override suspend fun createReview(orderItemId: String, rating: Int, content: String?): Result<Review> = failure("review")
    override suspend fun updateReview(reviewId: String, rating: Int, content: String?): Result<Review> = failure("review")

    private fun <T> failure(operation: String): Result<T> = Result.failure(AssertionError("$operation result not configured"))
}
