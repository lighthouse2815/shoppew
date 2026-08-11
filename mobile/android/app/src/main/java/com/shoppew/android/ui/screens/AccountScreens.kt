@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shoppew.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shoppew.android.core.api.Address
import com.shoppew.android.core.api.AddressRequest
import com.shoppew.android.core.api.AuthUser
import com.shoppew.android.core.api.Review
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.EmptyState
import com.shoppew.android.ui.common.ErrorState
import com.shoppew.android.ui.common.InlineMessage
import com.shoppew.android.ui.common.LoadState
import com.shoppew.android.ui.common.LoadingState
import com.shoppew.android.ui.common.ProductCard
import com.shoppew.android.ui.common.SectionTitle
import com.shoppew.android.ui.common.formatBusinessTime
import com.shoppew.android.ui.state.AccountViewModel

@Composable
fun AccountScreen(
    user: AuthUser,
    accountViewModel: AccountViewModel,
    onProfile: () -> Unit,
    onAddresses: () -> Unit,
    onWishlist: () -> Unit,
    onNotifications: () -> Unit,
    onReviews: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Tài khoản", style = MaterialTheme.typography.headlineSmall)
            Text(user.displayName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp))
            Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!user.emailVerified) InlineMessage("Email chưa được xác minh. Một số thao tác có thể bị giới hạn.", false, Modifier.padding(top = 10.dp))
        }
        HorizontalDivider()
        AccountMenu(Icons.Outlined.Person, "Hồ sơ cá nhân", "Tên hiển thị, số điện thoại", onProfile)
        AccountMenu(Icons.Outlined.LocationOn, "Địa chỉ nhận hàng", "Thêm và chọn địa chỉ mặc định", onAddresses)
        AccountMenu(Icons.Outlined.FavoriteBorder, "Sản phẩm đã lưu", "Danh sách yêu thích", onWishlist)
        AccountMenu(Icons.Outlined.Notifications, "Thông báo", "Đơn hàng, thanh toán và hệ thống", onNotifications)
        AccountMenu(Icons.Outlined.RateReview, "Đánh giá của tôi", "Xem và cập nhật đánh giá", onReviews)
        Spacer(Modifier.weight(1f))
        Text("Mã voucher được nhập và xác thực trực tiếp ở bước thanh toán.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Text("Đăng xuất", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AccountMenu(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Mở $title")
    }
    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
}

@Composable
fun ProfileScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadProfile(); viewModel.clearAction() }
    Scaffold(topBar = { TopAppBar(title = { Text("Hồ sơ cá nhân") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) }) { padding ->
        when (val profile = state.profile) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải hồ sơ…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(profile.message, viewModel::loadProfile, Modifier.padding(padding))
            is LoadState.Success -> {
                var name by rememberSaveable(profile.data.id) { mutableStateOf(profile.data.displayName) }
                var phone by rememberSaveable(profile.data.id) { mutableStateOf(profile.data.phone.orEmpty()) }
                var avatar by rememberSaveable(profile.data.id) { mutableStateOf(profile.data.avatarUrl.orEmpty()) }
                Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Tên hiển thị *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(avatar, { avatar = it }, label = { Text("URL ảnh đại diện") }, supportingText = { Text("Để trống nếu không dùng ảnh đại diện.") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(profile.data.email, {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), enabled = false)
                    when (val action = state.action) {
                        is ActionState.Error -> InlineMessage(action.message, true)
                        is ActionState.Success -> InlineMessage(action.message, false)
                        else -> Unit
                    }
                    Button(
                        onClick = { viewModel.updateProfile(name, phone, avatar) {} },
                        enabled = state.action != ActionState.Pending && name.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { if (state.action == ActionState.Pending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Lưu thay đổi") }
                }
            }
        }
    }
}

@Composable
fun AddressesScreen(viewModel: AccountViewModel, onBack: () -> Unit, onCreate: () -> Unit, onEdit: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<Address?>(null) }
    LaunchedEffect(Unit) { viewModel.loadAddresses(); viewModel.clearAction() }
    Scaffold(topBar = { TopAppBar(title = { Text("Địa chỉ nhận hàng") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val action = state.action) {
                is ActionState.Error -> InlineMessage(action.message, true, Modifier.padding(12.dp))
                is ActionState.Success -> InlineMessage(action.message, false, Modifier.padding(12.dp))
                else -> Unit
            }
            when (val addresses = state.addresses) {
                LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải địa chỉ…")
                is LoadState.Error -> ErrorState(addresses.message, viewModel::loadAddresses)
                is LoadState.Success -> if (addresses.data.isEmpty()) {
                    EmptyState("Chưa có địa chỉ", "Thêm địa chỉ để nhận hàng và xem trước phí giao.", "Thêm địa chỉ", onCreate)
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(addresses.data, key = { it.id }) { address ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row { Text(address.recipientName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); if (address.defaultAddress) Text("Mặc định", color = MaterialTheme.colorScheme.primary) }
                                    Text(address.phone)
                                    Text(listOfNotNull(address.addressLine, address.ward, address.district, address.province).joinToString(", "))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onEdit(address.id) }, modifier = Modifier.height(48.dp)) { Icon(Icons.Outlined.Edit, null); Text("Sửa") }
                                        if (!address.defaultAddress) TextButton(onClick = { viewModel.setDefaultAddress(address.id) }, modifier = Modifier.height(48.dp)) { Text("Đặt mặc định") }
                                        TextButton(onClick = { deleteTarget = address }, modifier = Modifier.height(48.dp)) { Icon(Icons.Outlined.DeleteOutline, null); Text("Xoá") }
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(12.dp).height(50.dp)) { Text("Thêm địa chỉ") }
                }
            }
        }
    }
    deleteTarget?.let { address ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá địa chỉ?") },
            text = { Text("Địa chỉ “${address.label ?: address.recipientName}” sẽ bị xoá khỏi tài khoản.") },
            confirmButton = { Button(onClick = { deleteTarget = null; viewModel.deleteAddress(address.id) }) { Text("Xoá") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") } },
        )
    }
}

@Composable
fun AddressEditorScreen(addressId: String?, viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val current = (state.addresses as? LoadState.Success)?.data?.firstOrNull { it.id == addressId }
    var label by rememberSaveable(addressId) { mutableStateOf("") }
    var recipient by rememberSaveable(addressId) { mutableStateOf("") }
    var phone by rememberSaveable(addressId) { mutableStateOf("") }
    var province by rememberSaveable(addressId) { mutableStateOf("") }
    var district by rememberSaveable(addressId) { mutableStateOf("") }
    var ward by rememberSaveable(addressId) { mutableStateOf("") }
    var line by rememberSaveable(addressId) { mutableStateOf("") }
    var default by rememberSaveable(addressId) { mutableStateOf(false) }
    LaunchedEffect(addressId) { if (addressId != null && state.addresses !is LoadState.Success) viewModel.loadAddresses(); viewModel.clearAction() }
    LaunchedEffect(current?.id) {
        current?.let {
            label = it.label.orEmpty(); recipient = it.recipientName; phone = it.phone; province = it.province
            district = it.district; ward = it.ward.orEmpty(); line = it.addressLine; default = it.defaultAddress
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (addressId == null) "Thêm địa chỉ" else "Sửa địa chỉ") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            OutlinedTextField(label, { label = it }, label = { Text("Nhãn địa chỉ") }, placeholder = { Text("Nhà, Công ty…") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(recipient, { recipient = it }, label = { Text("Người nhận *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(province, { province = it }, label = { Text("Tỉnh/thành phố *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(district, { district = it }, label = { Text("Quận/huyện *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ward, { ward = it }, label = { Text("Phường/xã") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(line, { line = it }, label = { Text("Địa chỉ chi tiết *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Đặt làm địa chỉ mặc định"); Text("Dùng sẵn khi thanh toán", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(default, { default = it })
            }
            when (val action = state.action) {
                is ActionState.Error -> InlineMessage(action.message, true)
                is ActionState.Success -> InlineMessage(action.message, false)
                else -> Unit
            }
            Button(
                onClick = { viewModel.saveAddress(addressId, AddressRequest(label.trim().takeIf(String::isNotEmpty), recipient.trim(), phone, province = province.trim(), district = district.trim(), ward = ward.trim().takeIf(String::isNotEmpty), addressLine = line.trim(), defaultAddress = default), onBack) },
                enabled = state.action != ActionState.Pending && recipient.isNotBlank() && phone.length >= 9 && province.isNotBlank() && district.isNotBlank() && line.length >= 5,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { if (state.action == ActionState.Pending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Lưu địa chỉ") }
        }
    }
}

@Composable
fun WishlistScreen(viewModel: AccountViewModel, onBack: () -> Unit, onProduct: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWishlist(); viewModel.clearAction() }
    Scaffold(topBar = { TopAppBar(title = { Text("Sản phẩm đã lưu") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) }) { padding ->
        when (val wishlist = state.wishlist) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải danh sách…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(wishlist.message, viewModel::loadWishlist, Modifier.padding(padding))
            is LoadState.Success -> if (wishlist.data.isEmpty()) EmptyState("Chưa lưu sản phẩm", "Nhấn biểu tượng yêu thích ở trang sản phẩm để lưu lại.") else {
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (val action = state.action) {
                        is ActionState.Error -> item { InlineMessage(action.message, true) }
                        is ActionState.Success -> item { InlineMessage(action.message, false) }
                        else -> Unit
                    }
                    items(wishlist.data, key = { it.id }) { entry ->
                        entry.product?.let { product ->
                            Column {
                                ProductCard(product, { onProduct(product.slug) }, Modifier.fillMaxWidth())
                                TextButton(onClick = { viewModel.removeWishlist(product.id) }, enabled = state.action != ActionState.Pending, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Bỏ lưu") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadNotifications(); viewModel.clearAction() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Thông báo") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } },
            actions = { TextButton(onClick = viewModel::readAllNotifications, enabled = state.action != ActionState.Pending) { Text("Đọc tất cả") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NotificationPermissionSection()
            HorizontalDivider()
            Box(Modifier.weight(1f)) {
                when (val notifications = state.notifications) {
                    LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải thông báo…")
                    is LoadState.Error -> ErrorState(notifications.message, viewModel::loadNotifications)
                    is LoadState.Success -> if (notifications.data.isEmpty()) EmptyState("Chưa có thông báo", "Cập nhật đơn hàng và thanh toán sẽ xuất hiện tại đây.") else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(notifications.data, key = { it.id }) { notification ->
                                Surface(
                                    color = if (notification.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = !notification.read) { viewModel.readNotification(notification.id) },
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Row { Text(notification.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text(notification.type, style = MaterialTheme.typography.labelMedium) }
                                        Text(notification.body)
                                        Text(formatBusinessTime(notification.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewsScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<Review?>(null) }
    LaunchedEffect(Unit) { viewModel.loadReviews(); viewModel.clearAction() }
    Scaffold(topBar = { TopAppBar(title = { Text("Đánh giá của tôi") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } }) }) { padding ->
        when (val reviews = state.reviews) {
            LoadState.Idle, LoadState.Loading -> LoadingState("Đang tải đánh giá…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(reviews.message, viewModel::loadReviews, Modifier.padding(padding))
            is LoadState.Success -> if (reviews.data.isEmpty()) EmptyState("Chưa có đánh giá", "Hoàn tất đơn hàng rồi chọn “Đánh giá sản phẩm” trong chi tiết đơn.") else {
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (val action = state.action) {
                        is ActionState.Error -> item { InlineMessage(action.message, true) }
                        is ActionState.Success -> item { InlineMessage(action.message, false) }
                        else -> Unit
                    }
                    items(reviews.data, key = { it.id }) { review ->
                        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("★".repeat(review.rating), color = MaterialTheme.colorScheme.primary)
                                Text(review.content ?: "Không có nội dung")
                                Text(formatBusinessTime(review.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                review.sellerReply?.let { InlineMessage("Phản hồi từ shop: $it", false) }
                                TextButton(onClick = { editing = review }, modifier = Modifier.height(48.dp)) { Icon(Icons.Outlined.Edit, null); Text("Chỉnh sửa") }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { review -> EditReviewDialog(review, state.action, { editing = null }) { rating, content -> viewModel.updateReview(review.id, rating, content) { editing = null } } }
}

@Composable
private fun EditReviewDialog(review: Review, action: ActionState, onDismiss: () -> Unit, onSubmit: (Int, String?) -> Unit) {
    var rating by rememberSaveable(review.id) { mutableIntStateOf(review.rating) }
    var content by rememberSaveable(review.id) { mutableStateOf(review.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa đánh giá") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row { (1..5).forEach { star -> IconButton(onClick = { rating = star }) { Icon(if (star <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, "$star sao", tint = MaterialTheme.colorScheme.primary) } } }
                OutlinedTextField(content, { content = it.take(2000) }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                if (action is ActionState.Error) InlineMessage(action.message, true)
            }
        },
        confirmButton = { Button(onClick = { onSubmit(rating, content.trim().takeIf(String::isNotEmpty)) }, enabled = action != ActionState.Pending) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = action != ActionState.Pending) { Text("Huỷ") } },
    )
}
