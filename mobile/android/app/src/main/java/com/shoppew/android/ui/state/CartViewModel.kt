package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.Cart
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

data class CartUiState(
    val cart: LoadState<Cart> = LoadState.Idle,
    val action: ActionState = ActionState.Idle,
)

@HiltViewModel
class CartViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun load() = mutate(showLoading = true) { repository.cart() }

    fun add(variantId: String, quantity: Long, onSuccess: () -> Unit = {}) = mutate(successMessage = "Đã thêm vào giỏ hàng", onSuccess = onSuccess) {
        repository.addCartItem(variantId, quantity)
    }

    fun update(itemId: String, quantity: Long) {
        if (quantity < 1) return
        mutate { repository.updateCartItem(itemId, quantity) }
    }

    fun select(itemId: String, selected: Boolean) = mutate { repository.selectCartItem(itemId, selected) }
    fun remove(itemId: String) = mutate(successMessage = "Đã xoá sản phẩm") { repository.removeCartItem(itemId) }
    fun clearAction() = _uiState.update { it.copy(action = ActionState.Idle) }

    private fun mutate(
        showLoading: Boolean = false,
        successMessage: String? = null,
        onSuccess: () -> Unit = {},
        call: suspend () -> Result<Cart>,
    ) {
        if (_uiState.value.action == ActionState.Pending) return
        viewModelScope.launch {
            _uiState.update { it.copy(cart = if (showLoading) LoadState.Loading else it.cart, action = ActionState.Pending) }
            call().fold(
                onSuccess = { cart ->
                    _uiState.value = CartUiState(LoadState.Success(cart), successMessage?.let(ActionState::Success) ?: ActionState.Idle)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            cart = if (showLoading) LoadState.Error(error.message ?: "Không tải được giỏ hàng") else state.cart,
                            action = ActionState.Error(error.message ?: "Thao tác không thành công"),
                        )
                    }
                },
            )
        }
    }
}
