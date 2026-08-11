package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.CheckoutPreview
import com.shoppew.android.core.api.CheckoutRequest
import com.shoppew.android.core.api.CheckoutResult
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

data class CheckoutUiState(
    val addresses: LoadState<List<Address>> = LoadState.Idle,
    val selectedAddressId: String? = null,
    val paymentProvider: String = "COD",
    val voucherCode: String = "",
    val customerNote: String = "",
    val preview: LoadState<CheckoutPreview> = LoadState.Idle,
    val action: ActionState = ActionState.Idle,
    val result: CheckoutResult? = null,
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()
    private var cartItemIds: List<String> = emptyList()

    fun start(itemIds: List<String>) {
        cartItemIds = itemIds
        if (_uiState.value.addresses != LoadState.Idle) return
        viewModelScope.launch {
            _uiState.update { it.copy(addresses = LoadState.Loading) }
            repository.addresses().fold(
                onSuccess = { addresses ->
                    val selected = addresses.firstOrNull { it.defaultAddress }?.id ?: addresses.firstOrNull()?.id
                    _uiState.update { it.copy(addresses = LoadState.Success(addresses), selectedAddressId = selected) }
                    if (selected != null) preview()
                },
                onFailure = { error -> _uiState.update { it.copy(addresses = LoadState.Error(error.message ?: "Không tải được địa chỉ")) } },
            )
        }
    }

    fun selectAddress(id: String) {
        _uiState.update { it.copy(selectedAddressId = id) }
        preview()
    }

    fun setPayment(provider: String) {
        _uiState.update { it.copy(paymentProvider = provider) }
        preview()
    }

    fun setVoucher(value: String) = _uiState.update { it.copy(voucherCode = value.uppercase()) }
    fun setNote(value: String) = _uiState.update { it.copy(customerNote = value) }
    fun applyVoucher() = preview()

    fun preview() {
        val request = requestOrNull() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(preview = LoadState.Loading, action = ActionState.Idle) }
            repository.previewCheckout(request).fold(
                { preview -> _uiState.update { it.copy(preview = LoadState.Success(preview)) } },
                { error -> _uiState.update { it.copy(preview = LoadState.Error(error.message ?: "Không thể tính đơn hàng")) } },
            )
        }
    }

    fun place() {
        if (_uiState.value.action == ActionState.Pending) return
        val request = requestOrNull()
        if (request == null) {
            _uiState.update { it.copy(action = ActionState.Error("Vui lòng chọn địa chỉ giao hàng.")) }
            return
        }
        if (_uiState.value.preview !is LoadState.Success) {
            _uiState.update { it.copy(action = ActionState.Error("Cần tải lại tổng tiền trước khi đặt hàng.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(action = ActionState.Pending) }
            repository.placeCheckout(request).fold(
                { result -> _uiState.update { it.copy(action = ActionState.Success("Đặt hàng thành công"), result = result) } },
                { error -> _uiState.update { it.copy(action = ActionState.Error(error.message ?: "Không thể đặt hàng")) } },
            )
        }
    }

    fun reset() {
        cartItemIds = emptyList()
        _uiState.value = CheckoutUiState()
    }

    private fun requestOrNull(): CheckoutRequest? {
        val state = _uiState.value
        val addressId = state.selectedAddressId ?: return null
        if (cartItemIds.isEmpty()) return null
        return CheckoutRequest(
            cartItemIds = cartItemIds,
            addressId = addressId,
            paymentProvider = state.paymentProvider,
            shippingMethodCode = null,
            customerNote = state.customerNote.trim().takeIf(String::isNotEmpty),
            voucherCodes = state.voucherCode.trim().takeIf(String::isNotEmpty)?.let(::listOf).orEmpty(),
        )
    }
}
