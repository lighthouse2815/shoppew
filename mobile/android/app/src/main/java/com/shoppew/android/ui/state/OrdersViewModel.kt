package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.OrderDetail
import com.shoppew.android.core.api.OrderSummary
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

data class OrdersUiState(
    val orders: LoadState<List<OrderSummary>> = LoadState.Idle,
    val detail: LoadState<OrderDetail> = LoadState.Idle,
    val action: ActionState = ActionState.Idle,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(orders = LoadState.Loading) }
            repository.orders().fold(
                { page -> _uiState.update { it.copy(orders = LoadState.Success(page.content)) } },
                { error -> _uiState.update { it.copy(orders = LoadState.Error(error.message ?: "Không tải được đơn hàng")) } },
            )
        }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(detail = LoadState.Loading, action = ActionState.Idle) }
            repository.order(orderId).fold(
                { order -> _uiState.update { it.copy(detail = LoadState.Success(order)) } },
                { error -> _uiState.update { it.copy(detail = LoadState.Error(error.message ?: "Không tải được đơn hàng")) } },
            )
        }
    }

    fun cancel(orderId: String, reason: String) = command(orderId, "Đã huỷ đơn hàng") { repository.cancelOrder(orderId, reason) }
    fun complete(orderId: String) = command(orderId, "Đã xác nhận hoàn tất") { repository.completeOrder(orderId) }

    private fun command(orderId: String, success: String, call: suspend () -> Result<OrderDetail>) {
        if (_uiState.value.action == ActionState.Pending) return
        viewModelScope.launch {
            _uiState.update { it.copy(action = ActionState.Pending) }
            call().fold(
                { order ->
                    _uiState.update { it.copy(detail = LoadState.Success(order), action = ActionState.Success(success)) }
                    loadOrders()
                },
                { error -> _uiState.update { it.copy(action = ActionState.Error(error.message ?: "Thao tác không thành công")) } },
            )
        }
    }
}
