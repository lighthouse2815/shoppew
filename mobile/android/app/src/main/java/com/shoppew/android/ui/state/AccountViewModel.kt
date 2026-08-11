package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.AddressRequest
import com.shoppew.android.core.api.NotificationItem
import com.shoppew.android.core.api.Profile
import com.shoppew.android.core.api.Review
import com.shoppew.android.core.api.UpdateProfileRequest
import com.shoppew.android.core.api.WishlistEntry
import com.shoppew.android.data.ShoppewRepository
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val profile: LoadState<Profile> = LoadState.Idle,
    val addresses: LoadState<List<Address>> = LoadState.Idle,
    val wishlist: LoadState<List<WishlistEntry>> = LoadState.Idle,
    val notifications: LoadState<List<NotificationItem>> = LoadState.Idle,
    val reviews: LoadState<List<Review>> = LoadState.Idle,
    val action: ActionState = ActionState.Idle,
)

@HiltViewModel
class AccountViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun loadProfile() = load(
        loading = { _uiState.update { it.copy(profile = LoadState.Loading) } },
        call = { repository.profile() },
        success = { profile -> _uiState.update { it.copy(profile = LoadState.Success(profile)) } },
        failure = { message -> _uiState.update { it.copy(profile = LoadState.Error(message)) } },
    )

    fun updateProfile(displayName: String, phone: String?, avatarUrl: String?, onSuccess: () -> Unit) {
        if (displayName.trim().length < 2) {
            _uiState.update { it.copy(action = ActionState.Error("Tên hiển thị cần ít nhất 2 ký tự.", mapOf("displayName" to "Tên quá ngắn"))) }
            return
        }
        action("Đã cập nhật hồ sơ", onSuccess, {
            repository.updateProfile(UpdateProfileRequest(displayName.trim(), avatarUrl?.trim()?.takeIf(String::isNotEmpty), phone = phone?.trim()?.takeIf(String::isNotEmpty)))
        }) { profile -> _uiState.update { it.copy(profile = LoadState.Success(profile)) } }
    }

    fun loadAddresses() = load(
        loading = { _uiState.update { it.copy(addresses = LoadState.Loading) } },
        call = { repository.addresses() },
        success = { addresses -> _uiState.update { it.copy(addresses = LoadState.Success(addresses)) } },
        failure = { message -> _uiState.update { it.copy(addresses = LoadState.Error(message)) } },
    )

    fun saveAddress(addressId: String?, request: AddressRequest, onSuccess: () -> Unit) {
        val validation = buildMap {
            if (request.recipientName.length < 2) put("recipientName", "Tên người nhận quá ngắn")
            if (request.phone.length < 9) put("phone", "Số điện thoại chưa hợp lệ")
            if (request.province.isBlank()) put("province", "Vui lòng nhập tỉnh/thành")
            if (request.district.isBlank()) put("district", "Vui lòng nhập quận/huyện")
            if (request.addressLine.length < 5) put("addressLine", "Địa chỉ chi tiết quá ngắn")
        }
        if (validation.isNotEmpty()) {
            _uiState.update { it.copy(action = ActionState.Error("Vui lòng kiểm tra địa chỉ.", validation)) }
            return
        }
        action(if (addressId == null) "Đã thêm địa chỉ" else "Đã cập nhật địa chỉ", onSuccess, {
            if (addressId == null) repository.createAddress(request) else repository.updateAddress(addressId, request)
        }) { loadAddresses() }
    }

    fun setDefaultAddress(addressId: String) = action("Đã đặt làm địa chỉ mặc định", call = { repository.setDefaultAddress(addressId) }) { loadAddresses() }

    fun deleteAddress(addressId: String) = action("Đã xoá địa chỉ", call = { repository.deleteAddress(addressId) }) { loadAddresses() }

    fun loadWishlist() = load(
        loading = { _uiState.update { it.copy(wishlist = LoadState.Loading) } },
        call = { repository.wishlist() },
        success = { wishlist -> _uiState.update { it.copy(wishlist = LoadState.Success(wishlist)) } },
        failure = { message -> _uiState.update { it.copy(wishlist = LoadState.Error(message)) } },
    )

    fun addWishlist(productId: String) = action("Đã lưu sản phẩm", call = { repository.addWishlist(productId) }) { loadWishlist() }
    fun removeWishlist(productId: String) = action("Đã bỏ lưu sản phẩm", call = { repository.removeWishlist(productId) }) { loadWishlist() }

    fun loadNotifications() = load(
        loading = { _uiState.update { it.copy(notifications = LoadState.Loading) } },
        call = { repository.notifications() },
        success = { page -> _uiState.update { it.copy(notifications = LoadState.Success(page.content)) } },
        failure = { message -> _uiState.update { it.copy(notifications = LoadState.Error(message)) } },
    )

    fun readNotification(notificationId: String) = action(null, call = { repository.readNotification(notificationId) }) { loadNotifications() }
    fun readAllNotifications() = action("Đã đánh dấu tất cả là đã đọc", call = { repository.readAllNotifications() }) { loadNotifications() }

    fun loadReviews() = load(
        loading = { _uiState.update { it.copy(reviews = LoadState.Loading) } },
        call = { repository.myReviews() },
        success = { page -> _uiState.update { it.copy(reviews = LoadState.Success(page.content)) } },
        failure = { message -> _uiState.update { it.copy(reviews = LoadState.Error(message)) } },
    )

    fun createReview(orderItemId: String, rating: Int, content: String?, onSuccess: () -> Unit) {
        if (rating !in 1..5) {
            _uiState.update { it.copy(action = ActionState.Error("Vui lòng chọn từ 1 đến 5 sao.")) }
            return
        }
        action("Đã gửi đánh giá", onSuccess, { repository.createReview(orderItemId, rating, content) }) { loadReviews() }
    }

    fun updateReview(reviewId: String, rating: Int, content: String?, onSuccess: () -> Unit) =
        action("Đã cập nhật đánh giá", onSuccess, { repository.updateReview(reviewId, rating, content) }) { loadReviews() }

    fun clearAction() = _uiState.update { it.copy(action = ActionState.Idle) }

    private fun <T> load(
        loading: () -> Unit,
        call: suspend () -> Result<T>,
        success: (T) -> Unit,
        failure: (String) -> Unit,
    ) {
        viewModelScope.launch {
            loading()
            call().fold(success, { failure(it.message ?: "Không tải được dữ liệu") })
        }
    }

    private fun <T> action(
        successMessage: String?,
        onSuccess: () -> Unit = {},
        call: suspend () -> Result<T>,
        after: (T) -> Unit,
    ) {
        if (_uiState.value.action == ActionState.Pending) return
        viewModelScope.launch {
            _uiState.update { it.copy(action = ActionState.Pending) }
            call().fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(action = successMessage?.let(ActionState::Success) ?: ActionState.Idle) }
                    after(result)
                    onSuccess()
                },
                onFailure = { error -> _uiState.update { it.copy(action = ActionState.Error(error.message ?: "Thao tác không thành công")) } },
            )
        }
    }
}
