package com.shoppew.android.ui.state

import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.Cart
import com.shoppew.android.core.api.CheckoutPreview
import com.shoppew.android.core.api.CheckoutResult
import com.shoppew.android.core.api.OrderDetail
import com.shoppew.android.core.api.OrderSummary
import com.shoppew.android.core.api.PageResponse
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.data.ShoppewException
import com.shoppew.android.test.MainDispatcherRule
import com.shoppew.android.test.TestShoppewRepository
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommerceViewModelsTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `catalog loads real repository page and keeps pagination metadata`() = runTest(mainDispatcherRule.dispatcher) {
        val product = ProductSummary(id = "product-1", name = "Áo cotton", slug = "ao-cotton")
        val repository = TestShoppewRepository().apply {
            productsResult = Result.success(PageResponse(listOf(product), page = 0, size = 24, totalElements = 49, totalPages = 3))
        }

        val viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        val products = viewModel.uiState.value.products as LoadState.Success
        assertEquals(listOf(product), products.data)
        assertEquals(3, viewModel.uiState.value.totalPages)
    }

    @Test
    fun `cart surfaces server state after load`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository().apply {
            cartResult = Result.success(Cart(id = "cart-1", itemCount = 2, selectedItemCount = 1))
        }
        val viewModel = CartViewModel(repository)

        viewModel.load()
        advanceUntilIdle()

        val cart = (viewModel.uiState.value.cart as LoadState.Success).data
        assertEquals("cart-1", cart.id)
        assertEquals(2, cart.itemCount)
        assertEquals(ActionState.Idle, viewModel.uiState.value.action)
    }

    @Test
    fun `checkout previews authoritative totals before placing order`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository().apply {
            addressResult = Result.success(listOf(Address(id = "address-1", defaultAddress = true)))
            previewResult = Result.success(CheckoutPreview(paymentProvider = "COD"))
            checkoutResult = Result.success(CheckoutResult(id = "checkout-1", orders = listOf(OrderSummary(id = "order-1"))))
        }
        val viewModel = CheckoutViewModel(repository)

        viewModel.start(listOf("cart-item-1"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.preview is LoadState.Success)
        assertEquals("address-1", repository.previewRequests.single().addressId)

        viewModel.place()
        advanceUntilIdle()

        assertEquals("checkout-1", viewModel.uiState.value.result?.id)
        assertEquals(listOf("cart-item-1"), repository.checkoutRequests.single().cartItemIds)
        assertTrue(viewModel.uiState.value.action is ActionState.Success)
    }

    @Test
    fun `checkout refuses submission before an authoritative preview`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository().apply {
            addressResult = Result.success(listOf(Address(id = "address-1", defaultAddress = true)))
            previewResult = Result.failure(ShoppewException("INSUFFICIENT_STOCK", "Sản phẩm đã hết hàng"))
        }
        val viewModel = CheckoutViewModel(repository)

        viewModel.start(listOf("cart-item-1"))
        advanceUntilIdle()
        viewModel.place()

        assertTrue(viewModel.uiState.value.preview is LoadState.Error)
        assertTrue(viewModel.uiState.value.action is ActionState.Error)
        assertTrue(repository.checkoutRequests.isEmpty())
    }

    @Test
    fun `orders expose backend error and recover on retry`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository().apply {
            ordersResult = Result.failure(ShoppewException("NETWORK_ERROR", "Mất kết nối"))
        }
        val viewModel = OrdersViewModel(repository)

        viewModel.loadOrders()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.orders is LoadState.Error)

        repository.ordersResult = Result.success(PageResponse(listOf(OrderSummary(id = "order-1", status = "CONFIRMED"))))
        repository.orderResult = Result.success(OrderDetail(id = "order-1", status = "CONFIRMED"))
        viewModel.loadOrders()
        viewModel.loadOrder("order-1")
        advanceUntilIdle()

        assertEquals("order-1", (viewModel.uiState.value.orders as LoadState.Success).data.single().id)
        assertEquals("order-1", (viewModel.uiState.value.detail as LoadState.Success).data.id)
    }
}
