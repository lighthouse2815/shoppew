@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shoppew.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.Cart
import com.shoppew.android.core.api.CartItem
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.EmptyState
import com.shoppew.android.ui.common.ErrorState
import com.shoppew.android.ui.common.InlineMessage
import com.shoppew.android.ui.common.LoadState
import com.shoppew.android.ui.common.LoadingState
import com.shoppew.android.ui.common.ProductImage
import com.shoppew.android.ui.common.SectionTitle
import com.shoppew.android.ui.common.formatMoney
import com.shoppew.android.ui.state.CartViewModel
import com.shoppew.android.ui.state.CheckoutViewModel

@Composable
fun CartScreen(viewModel: CartViewModel, onProduct: (String) -> Unit, onCheckout: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val cart = (state.cart as? LoadState.Success)?.data
    Scaffold(
        topBar = { TopAppBar(title = { Text("Giỏ hàng") }) },
        bottomBar = {
            if (cart != null && cart.shops.isNotEmpty()) {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${cart.selectedItemCount} sản phẩm đã chọn", style = MaterialTheme.typography.labelMedium)
                            Text(formatMoney(cart.selectedSubtotal, cart.currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = onCheckout,
                            enabled = cart.selectedItemCount > 0 && cart.selectedItems.all { it.eligible } && state.action != ActionState.Pending,
                            modifier = Modifier.height(50.dp),
                        ) { Text("Thanh toán") }
                    }
                }
            }
        },
    ) { padding ->
        when (val load = state.cart) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang kiểm tra giỏ hàng…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(load.message, viewModel::load, Modifier.padding(padding))
            is LoadState.Success -> CartContent(load.data, state.action, viewModel, onProduct, Modifier.padding(padding))
        }
    }
}

@Composable
private fun CartContent(cart: Cart, action: ActionState, viewModel: CartViewModel, onProduct: (String) -> Unit, modifier: Modifier) {
    if (cart.shops.isEmpty()) {
        EmptyState("Giỏ hàng đang trống", "Hãy chọn một sản phẩm phù hợp để bắt đầu.")
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        when (action) {
            is ActionState.Error -> item { InlineMessage(action.message, true, Modifier.padding(12.dp)) }
            is ActionState.Success -> item { InlineMessage(action.message, false, Modifier.padding(12.dp)) }
            else -> Unit
        }
        cart.shops.forEach { shop ->
            item(key = "shop-${shop.shopId}") {
                Text(shop.shopName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
            }
            items(shop.items, key = { it.id }) { item ->
                CartItemRow(item, pending = action == ActionState.Pending, onProduct = { onProduct(item.productSlug) }, onSelect = { viewModel.select(item.id, it) }, onQuantity = { viewModel.update(item.id, it) }, onRemove = { viewModel.remove(item.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    pending: Boolean,
    onProduct: () -> Unit,
    onSelect: (Boolean) -> Unit,
    onQuantity: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
        Checkbox(checked = item.selected, onCheckedChange = onSelect, enabled = !pending && item.eligible)
        ProductImage(item.imageUrl, item.productName, Modifier.size(88.dp).clickable(onClick = onProduct))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.productName, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = onProduct))
            Text(item.variantName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(item.unitPrice, item.currency), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            if (!item.eligible) InlineMessage(item.issues.joinToString(" · ").ifBlank { "Sản phẩm hiện không thể thanh toán" }, true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantity(item.quantity - 1) }, enabled = !pending && item.quantity > 1) { Icon(Icons.Outlined.Remove, "Giảm số lượng") }
                Text(item.quantity.toString(), modifier = Modifier.width(32.dp))
                IconButton(onClick = { onQuantity(item.quantity + 1) }, enabled = !pending && item.quantity < item.availableQuantity) { Icon(Icons.Outlined.Add, "Tăng số lượng") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }, enabled = !pending) { Icon(Icons.Outlined.DeleteOutline, "Xoá khỏi giỏ") }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá khỏi giỏ hàng?") },
            text = { Text("Sản phẩm sẽ được xoá khỏi giỏ, bạn vẫn có thể thêm lại sau.") },
            confirmButton = { Button(onClick = { confirmDelete = false; onRemove() }) { Text("Xoá") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Giữ lại") } },
        )
    }
}

@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onOrder: (String) -> Unit,
) {
    val cartState by cartViewModel.uiState.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val cart = (cartState.cart as? LoadState.Success)?.data
    val selectedIds = cart?.selectedItems?.map { it.id }.orEmpty()
    LaunchedEffect(selectedIds) {
        viewModel.reset()
        viewModel.start(selectedIds)
    }
    state.result?.let { result ->
        CheckoutSuccessScreen(
            checkoutNumber = result.checkoutNumber,
            total = formatMoney(result.grandTotal, result.currency),
            onOrder = { result.orders.firstOrNull()?.id?.let(onOrder) },
        )
        return
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Thanh toán") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) },
        bottomBar = {
            val preview = (state.preview as? LoadState.Success)?.data
            Surface(shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tổng thanh toán", style = MaterialTheme.typography.labelMedium)
                        Text(formatMoney(preview?.grandTotal, preview?.currency ?: "VND"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = viewModel::place,
                        enabled = preview != null && state.selectedAddressId != null && state.action != ActionState.Pending,
                        modifier = Modifier.height(50.dp),
                    ) {
                        if (state.action == ActionState.Pending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Đặt hàng")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SectionTitle("Địa chỉ giao hàng", "Thêm địa chỉ", onAddAddress) }
            when (val addresses = state.addresses) {
                LoadState.Idle, LoadState.Loading -> item { LoadingState("Đang tải địa chỉ…", Modifier.height(120.dp)) }
                is LoadState.Error -> item { ErrorState(addresses.message, { viewModel.start(selectedIds) }) }
                is LoadState.Success -> if (addresses.data.isEmpty()) {
                    item { EmptyState("Chưa có địa chỉ", "Thêm địa chỉ nhận hàng trước khi tiếp tục.", "Thêm địa chỉ", onAddAddress) }
                } else {
                    items(addresses.data, key = { it.id }) { address -> AddressChoice(address, state.selectedAddressId == address.id) { viewModel.selectAddress(address.id) } }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Phương thức thanh toán")
                    PaymentChoice("COD", "Thanh toán khi nhận hàng", state.paymentProvider == "COD") { viewModel.setPayment("COD") }
                    PaymentChoice("MOCK_ONLINE", "Thanh toán trực tuyến mô phỏng", state.paymentProvider == "MOCK_ONLINE") { viewModel.setPayment("MOCK_ONLINE") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Mã ưu đãi")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(state.voucherCode, viewModel::setVoucher, label = { Text("Mã voucher") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = viewModel::applyVoucher, enabled = state.voucherCode.isNotBlank() && state.preview !is LoadState.Loading, modifier = Modifier.height(56.dp)) { Text("Áp dụng") }
                    }
                    Text("Voucher hợp lệ sẽ được máy chủ áp dụng và thể hiện trong tổng tiền.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { OutlinedTextField(state.customerNote, viewModel::setNote, label = { Text("Ghi chú cho người bán") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4) }
            when (val preview = state.preview) {
                LoadState.Idle -> Unit
                LoadState.Loading -> item { LoadingState("Đang kiểm tra giá, kho và phí giao hàng…", Modifier.height(140.dp)) }
                is LoadState.Error -> item { ErrorState(preview.message, viewModel::preview) }
                is LoadState.Success -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Tóm tắt thanh toán")
                        SummaryRow("Tiền hàng", formatMoney(preview.data.itemsSubtotal, preview.data.currency))
                        SummaryRow("Phí giao hàng", formatMoney(preview.data.shippingTotal, preview.data.currency))
                        SummaryRow("Giảm giá", "− ${formatMoney(preview.data.discountTotal, preview.data.currency)}")
                        HorizontalDivider()
                        SummaryRow("Tổng cộng", formatMoney(preview.data.grandTotal, preview.data.currency), bold = true)
                        preview.data.appliedVouchers.forEach { Text("Đã áp dụng ${it.code}: −${formatMoney(it.discountAmount, it.currency)}", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            when (val action = state.action) {
                is ActionState.Error -> item { InlineMessage(action.message, true) }
                is ActionState.Success -> item { InlineMessage(action.message, false) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AddressChoice(address: Address, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).semantics { role = Role.RadioButton },
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected, onSelect)
            Column(Modifier.padding(start = 6.dp)) {
                Text(address.recipientName, fontWeight = FontWeight.SemiBold)
                Text(address.phone)
                Text(listOfNotNull(address.addressLine, address.ward, address.district, address.province).joinToString(", "))
                if (address.defaultAddress) Text("Mặc định", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PaymentChoice(code: String, label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).semantics { role = Role.RadioButton }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onSelect)
        Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp))
        Column { Text(label); Text(code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CheckoutSuccessScreen(checkoutNumber: String, total: String, onOrder: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Đặt hàng thành công", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Mã thanh toán: $checkoutNumber", modifier = Modifier.padding(top = 10.dp))
        Text("Tổng cộng $total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        Text("Trạng thái mới nhất sẽ hiển thị trong chi tiết đơn hàng.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
        Button(onClick = onOrder, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Xem đơn hàng") }
    }
}
