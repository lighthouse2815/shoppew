@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shoppew.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoppew.android.core.api.OrderDetail
import com.shoppew.android.core.api.OrderItem
import com.shoppew.android.core.api.OrderSummary
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.EmptyState
import com.shoppew.android.ui.common.ErrorState
import com.shoppew.android.ui.common.InlineMessage
import com.shoppew.android.ui.common.KeyValueRow
import com.shoppew.android.ui.common.LoadState
import com.shoppew.android.ui.common.LoadingState
import com.shoppew.android.ui.common.ProductImage
import com.shoppew.android.ui.common.SectionTitle
import com.shoppew.android.ui.common.formatBusinessTime
import com.shoppew.android.ui.common.formatMoney
import com.shoppew.android.ui.common.orderStatusLabel
import com.shoppew.android.ui.state.AccountViewModel
import com.shoppew.android.ui.state.OrdersViewModel

@Composable
fun OrdersScreen(viewModel: OrdersViewModel, onOrder: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadOrders() }
    Column(Modifier.fillMaxSize()) {
        Text("Đơn mua", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        when (val orders = state.orders) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải đơn hàng…")
            is LoadState.Error -> ErrorState(orders.message, viewModel::loadOrders)
            is LoadState.Success -> if (orders.data.isEmpty()) EmptyState("Chưa có đơn hàng", "Đơn đã đặt sẽ xuất hiện tại đây.") else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(orders.data, key = { it.id }) { order -> OrderSummaryCard(order) { onOrder(order.id) } }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: OrderSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(order.shopName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(orderStatusLabel(order.status), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Text(order.orderNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${order.itemCount} sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                Text(formatBusinessTime(order.placedAt), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(formatMoney(order.grandTotal, order.currency), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderDetailScreen(orderId: String, viewModel: OrdersViewModel, accountViewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val accountState by accountViewModel.uiState.collectAsState()
    var showCancel by rememberSaveable { mutableStateOf(false) }
    var reviewItem by remember { mutableStateOf<OrderItem?>(null) }
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Chi tiết đơn") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        when (val detail = state.detail) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải đơn hàng…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(detail.message, { viewModel.loadOrder(orderId) }, Modifier.padding(padding))
            is LoadState.Success -> OrderDetailContent(
                detail.data,
                state.action,
                accountState.action,
                onCancel = { showCancel = true },
                onComplete = { viewModel.complete(detail.data.id) },
                onReview = { reviewItem = it },
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (showCancel) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Huỷ đơn hàng?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đơn chỉ được huỷ khi trạng thái hiện tại cho phép. Hành động này không thể hoàn tác.")
                    OutlinedTextField(reason, { reason = it }, label = { Text("Lý do huỷ *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = { Button(onClick = { showCancel = false; viewModel.cancel(orderId, reason.trim()) }, enabled = reason.trim().length >= 3) { Text("Xác nhận huỷ") } },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Giữ đơn") } },
        )
    }
    reviewItem?.let { item ->
        ReviewDialog(item, accountState.action, onDismiss = { reviewItem = null }, onSubmit = { rating, content ->
            accountViewModel.createReview(item.id, rating, content) { reviewItem = null }
        })
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderDetail,
    orderAction: ActionState,
    reviewAction: ActionState,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onReview: (OrderItem) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(orderStatusLabel(order.status), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(order.orderNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Đặt lúc ${formatBusinessTime(order.placedAt)}", style = MaterialTheme.typography.labelMedium)
            }
        }
        when (orderAction) {
            is ActionState.Error -> item { InlineMessage(orderAction.message, true) }
            is ActionState.Success -> item { InlineMessage(orderAction.message, false) }
            else -> Unit
        }
        when (reviewAction) {
            is ActionState.Error -> item { InlineMessage(reviewAction.message, true) }
            is ActionState.Success -> item { InlineMessage(reviewAction.message, false) }
            else -> Unit
        }
        item { SectionTitle("Sản phẩm từ ${order.shopName}") }
        items(order.items, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                ProductImage(item.imageUrl, item.productName, Modifier.size(80.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.productName, fontWeight = FontWeight.SemiBold)
                    Text(item.variantName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.quantity} × ${formatMoney(item.unitPrice, item.currency)}")
                    Text(formatMoney(item.subtotal, item.currency), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    if (order.status == "COMPLETED") {
                        TextButton(onClick = { onReview(item) }, modifier = Modifier.height(48.dp)) { Text("Đánh giá sản phẩm") }
                    }
                }
            }
            HorizontalDivider()
        }
        item {
            Column {
                SectionTitle("Thanh toán")
                KeyValueRow("Tiền hàng", formatMoney(order.itemsSubtotal, order.currency))
                KeyValueRow("Phí giao hàng", formatMoney(order.shippingTotal, order.currency))
                KeyValueRow("Giảm giá", formatMoney(order.shopDiscountTotal + order.platformDiscountTotal, order.currency))
                KeyValueRow("Tổng cộng", formatMoney(order.grandTotal, order.currency))
            }
        }
        order.address?.let { address ->
            item {
                Column {
                    SectionTitle("Giao đến")
                    Text(address.recipientName, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                    Text(address.phone)
                    Text(listOfNotNull(address.addressLine, address.ward, address.district, address.province).joinToString(", "))
                }
            }
        }
        order.shipment?.let { shipment ->
            item {
                Column {
                    SectionTitle("Vận chuyển")
                    KeyValueRow("Đơn vị", shipment.provider)
                    KeyValueRow("Phương thức", shipment.methodName)
                    if (!shipment.trackingNumber.isNullOrBlank()) KeyValueRow("Mã vận đơn", shipment.trackingNumber)
                    KeyValueRow("Trạng thái", shipment.status)
                }
            }
        }
        if (order.history.isNotEmpty()) {
            item { SectionTitle("Lịch sử trạng thái") }
            items(order.history) { history ->
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(orderStatusLabel(history.toStatus), fontWeight = FontWeight.SemiBold)
                        history.reason?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text(formatBusinessTime(history.createdAt), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (order.status in setOf("PENDING_PAYMENT", "PAID", "CONFIRMED")) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), enabled = orderAction != ActionState.Pending) { Text("Huỷ đơn") }
                }
                if (order.status == "DELIVERED") {
                    Button(onClick = onComplete, modifier = Modifier.weight(1f).height(50.dp), enabled = orderAction != ActionState.Pending) { Text("Đã nhận hàng") }
                }
            }
        }
    }
}

@Composable
private fun ReviewDialog(item: OrderItem, action: ActionState, onDismiss: () -> Unit, onSubmit: (Int, String?) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var content by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (action != ActionState.Pending) onDismiss() },
        title = { Text("Đánh giá ${item.productName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }, modifier = Modifier.semantics { contentDescription = "$star sao" }) {
                            Icon(if (star <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                OutlinedTextField(content, { content = it.take(2000) }, label = { Text("Chia sẻ trải nghiệm") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                if (action is ActionState.Error) InlineMessage(action.message, true)
            }
        },
        confirmButton = { Button(onClick = { onSubmit(rating, content.trim().takeIf(String::isNotEmpty)) }, enabled = action != ActionState.Pending) { Text("Gửi đánh giá") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = action != ActionState.Pending) { Text("Để sau") } },
    )
}
