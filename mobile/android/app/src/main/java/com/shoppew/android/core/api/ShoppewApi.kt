package com.shoppew.android.core.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ShoppewApi {
    @GET("api/v1/public/categories")
    suspend fun categories(): ApiEnvelope<List<Category>>

    @GET("api/v1/public/products")
    suspend fun products(
        @Query("q") query: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 24,
    ): ApiEnvelope<PageResponse<ProductSummary>>

    @GET("api/v1/public/products/{slug}")
    suspend fun product(@Path("slug") slug: String): ApiEnvelope<ProductDetail>

    @GET("api/v1/public/products/{productId}/reviews")
    suspend fun productReviews(
        @Path("productId") productId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<PageResponse<Review>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(): ApiEnvelope<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun me(): ApiEnvelope<AuthUser>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiEnvelope<Map<String, Boolean>>

    @GET("api/v1/users/me/profile")
    suspend fun profile(): ApiEnvelope<Profile>

    @PUT("api/v1/users/me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiEnvelope<Profile>

    @GET("api/v1/users/me/addresses")
    suspend fun addresses(): ApiEnvelope<List<Address>>

    @POST("api/v1/users/me/addresses")
    suspend fun createAddress(@Body request: AddressRequest): ApiEnvelope<Address>

    @PUT("api/v1/users/me/addresses/{addressId}")
    suspend fun updateAddress(
        @Path("addressId") addressId: String,
        @Body request: AddressRequest,
    ): ApiEnvelope<Address>

    @DELETE("api/v1/users/me/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: String): ApiEnvelope<Map<String, Boolean>>

    @PATCH("api/v1/users/me/addresses/{addressId}/default")
    suspend fun setDefaultAddress(@Path("addressId") addressId: String): ApiEnvelope<Address>

    @GET("api/v1/cart")
    suspend fun cart(): ApiEnvelope<Cart>

    @POST("api/v1/cart/items")
    suspend fun addCartItem(@Body request: CartItemRequest): ApiEnvelope<Cart>

    @PUT("api/v1/cart/items/{itemId}")
    suspend fun updateCartItem(
        @Path("itemId") itemId: String,
        @Body request: CartQuantityRequest,
    ): ApiEnvelope<Cart>

    @DELETE("api/v1/cart/items/{itemId}")
    suspend fun removeCartItem(@Path("itemId") itemId: String): ApiEnvelope<Cart>

    @PATCH("api/v1/cart/items/{itemId}/selection")
    suspend fun selectCartItem(
        @Path("itemId") itemId: String,
        @Body request: CartSelectionRequest,
    ): ApiEnvelope<Cart>

    @POST("api/v1/checkout/preview")
    suspend fun previewCheckout(@Body request: CheckoutRequest): ApiEnvelope<CheckoutPreview>

    @POST("api/v1/checkout")
    suspend fun placeCheckout(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CheckoutRequest,
    ): ApiEnvelope<CheckoutResult>

    @GET("api/v1/orders")
    suspend fun orders(@Query("page") page: Int = 0, @Query("size") size: Int = 20): ApiEnvelope<PageResponse<OrderSummary>>

    @GET("api/v1/orders/{orderId}")
    suspend fun order(@Path("orderId") orderId: String): ApiEnvelope<OrderDetail>

    @POST("api/v1/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Body request: OrderActionRequest,
    ): ApiEnvelope<OrderDetail>

    @POST("api/v1/orders/{orderId}/complete")
    suspend fun completeOrder(@Path("orderId") orderId: String): ApiEnvelope<OrderDetail>

    @GET("api/v1/wishlist")
    suspend fun wishlist(): ApiEnvelope<List<WishlistEntry>>

    @POST("api/v1/wishlist/products/{productId}")
    suspend fun addWishlist(@Path("productId") productId: String): ApiEnvelope<WishlistEntry>

    @DELETE("api/v1/wishlist/products/{productId}")
    suspend fun removeWishlist(@Path("productId") productId: String): Response<Unit>

    @GET("api/v1/notifications")
    suspend fun notifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<PageResponse<NotificationItem>>

    @GET("api/v1/notifications/unread-count")
    suspend fun unreadCount(): ApiEnvelope<UnreadCount>

    @POST("api/v1/notifications/{notificationId}/read")
    suspend fun readNotification(@Path("notificationId") notificationId: String): ApiEnvelope<NotificationItem>

    @POST("api/v1/notifications/read-all")
    suspend fun readAllNotifications(): ApiEnvelope<UnreadCount>

    @PUT("api/v1/notifications/devices/current")
    suspend fun registerPushDevice(@Body request: PushDeviceRequest): ApiEnvelope<PushDevice>

    @HTTP(method = "DELETE", path = "api/v1/notifications/devices/current", hasBody = true)
    suspend fun unregisterPushDevice(@Body request: PushDeviceRevocationRequest): ApiEnvelope<Map<String, Boolean>>

    @GET("api/v1/reviews/me")
    suspend fun myReviews(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<PageResponse<Review>>

    @POST("api/v1/reviews")
    suspend fun createReview(@Body request: ReviewRequest): ApiEnvelope<Review>

    @PUT("api/v1/reviews/{reviewId}")
    suspend fun updateReview(
        @Path("reviewId") reviewId: String,
        @Body request: ReviewUpdateRequest,
    ): ApiEnvelope<Review>
}
