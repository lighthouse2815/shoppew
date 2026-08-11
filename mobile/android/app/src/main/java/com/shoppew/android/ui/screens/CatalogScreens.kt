@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shoppew.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductVariant
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.EmptyState
import com.shoppew.android.ui.common.ErrorState
import com.shoppew.android.ui.common.InlineMessage
import com.shoppew.android.ui.common.LoadState
import com.shoppew.android.ui.common.LoadingState
import com.shoppew.android.ui.common.ProductCard
import com.shoppew.android.ui.common.ProductImage
import com.shoppew.android.ui.common.SectionTitle
import com.shoppew.android.ui.common.formatBusinessTime
import com.shoppew.android.ui.common.formatMoney
import com.shoppew.android.ui.state.AccountViewModel
import com.shoppew.android.ui.state.CartViewModel
import com.shoppew.android.ui.state.CatalogViewModel

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    homeMode: Boolean,
    onProduct: (String) -> Unit,
    onSearch: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("shoppew", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm sản phẩm, thương hiệu…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search()
                    if (homeMode) onSearch?.invoke()
                }),
            )
            if (!homeMode && state.query.isBlank() && state.recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tìm gần đây", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearSearchHistory) { Text("Xoá") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recentSearches, key = { it }) { suggestion ->
                        AssistChip(onClick = { viewModel.useSearchSuggestion(suggestion) }, label = { Text(suggestion) })
                    }
                }
            }
        }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (homeMode) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Mua sắm rõ ràng, giao dịch an tâm", style = MaterialTheme.typography.titleLarge)
                                Text("Giá, tồn kho, phí giao hàng và ưu đãi được máy chủ kiểm tra lại khi thanh toán.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    SectionTitle(if (homeMode) "Danh mục" else "Lọc theo danh mục")
                    when (val categories = state.categories) {
                        LoadState.Loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                        is LoadState.Error -> ErrorState(categories.message, viewModel::loadCategories)
                        is LoadState.Success -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = state.selectedCategoryId == null, onClick = { viewModel.selectCategory(null) }, label = { Text("Tất cả") }) }
                            items(flattenCategories(categories.data), key = { it.id }) { category ->
                                FilterChip(selected = state.selectedCategoryId == category.id, onClick = { viewModel.selectCategory(category.id) }, label = { Text(category.name) })
                            }
                        }
                        LoadState.Idle -> Unit
                    }
                    SectionTitle(if (state.query.isBlank()) "Sản phẩm mới" else "Kết quả cho “${state.query}”")
                }
            }
            if (homeMode && state.recentlyViewed.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Đã xem gần đây")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.recentlyViewed, key = { "recent-${it.id}" }) { product ->
                                ProductCard(product, { onProduct(product.slug) }, Modifier.width(172.dp))
                            }
                        }
                    }
                }
            }
            when (val products = state.products) {
                LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingState("Đang tải sản phẩm…", Modifier.height(240.dp)) }
                is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) { ErrorState(products.message, onRetry = { viewModel.loadProducts() }) }
                is LoadState.Success -> {
                    if (products.data.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("Chưa tìm thấy sản phẩm", "Hãy thử từ khoá khác hoặc chọn danh mục rộng hơn.", "Xoá bộ lọc") { viewModel.setQuery(""); viewModel.selectCategory(null) } }
                    } else {
                        items(products.data, key = { it.id }) { product -> ProductCard(product, { onProduct(product.slug) }) }
                        if (state.currentPage + 1 < state.totalPages) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                OutlinedButton(onClick = { viewModel.loadProducts(reset = false) }, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                    if (state.loadingMore) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Xem thêm")
                                }
                            }
                        }
                    }
                }
                LoadState.Idle -> Unit
            }
            }
        }
    }
}

private fun flattenCategories(categories: List<com.shoppew.android.core.api.Category>): List<com.shoppew.android.core.api.Category> =
    categories.flatMap { listOf(it) + flattenCategories(it.children) }

@Composable
fun ProductDetailScreen(
    slug: String,
    viewModel: CatalogViewModel,
    cartViewModel: CartViewModel,
    accountViewModel: AccountViewModel,
    authenticated: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onCart: () -> Unit,
) {
    val productState by viewModel.productState.collectAsState()
    val cartState by cartViewModel.uiState.collectAsState()
    val accountState by accountViewModel.uiState.collectAsState()
    LaunchedEffect(slug) {
        cartViewModel.clearAction()
        accountViewModel.clearAction()
        viewModel.loadProduct(slug)
    }
    val detail = (productState.detail as? LoadState.Success)?.data
    var variantId by rememberSaveable(slug) { mutableStateOf<String?>(null) }
    var quantity by rememberSaveable(slug) { mutableLongStateOf(1) }
    LaunchedEffect(detail?.id) {
        variantId = detail?.variants?.firstOrNull { it.status == "ACTIVE" }?.id
    }
    val selectedVariant = detail?.variants?.firstOrNull { it.id == variantId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: "Chi tiết sản phẩm", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } },
                actions = { IconButton(onClick = onCart) { Icon(Icons.Outlined.ShoppingCart, "Mở giỏ hàng") } },
            )
        },
        bottomBar = {
            if (detail != null) {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Giá hiện tại", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMoney(selectedVariant?.price, selectedVariant?.currency ?: "VND"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(
                            onClick = { if (authenticated) accountViewModel.addWishlist(detail.id) else onLogin() },
                            enabled = accountState.action != ActionState.Pending,
                            modifier = Modifier.height(50.dp),
                        ) { Icon(Icons.Outlined.FavoriteBorder, "Lưu sản phẩm") }
                        Button(
                            onClick = {
                                if (!authenticated) onLogin() else selectedVariant?.let { cartViewModel.add(it.id, quantity, onCart) }
                            },
                            enabled = selectedVariant != null && cartState.action != ActionState.Pending,
                            modifier = Modifier.height(50.dp),
                        ) { Text(if (selectedVariant == null) "Chọn phân loại" else "Thêm vào giỏ") }
                    }
                }
            }
        },
    ) { padding ->
        when (val state = productState.detail) {
            LoadState.Loading, LoadState.Idle -> LoadingState("Đang tải sản phẩm…", Modifier.padding(padding))
            is LoadState.Error -> ErrorState(state.message, { viewModel.loadProduct(slug) }, Modifier.padding(padding))
            is LoadState.Success -> ProductDetailContent(
                state.data,
                productState.reviews,
                selectedVariant,
                onVariant = { variantId = it.id },
                quantity = quantity,
                onQuantity = { quantity = it.coerceIn(1, 99) },
                cartAction = cartState.action,
                accountAction = accountState.action,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDetail,
    reviews: LoadState<List<com.shoppew.android.core.api.Review>>,
    selectedVariant: ProductVariant?,
    onVariant: (ProductVariant) -> Unit,
    quantity: Long,
    onQuantity: (Long) -> Unit,
    cartAction: ActionState,
    accountAction: ActionState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            if (product.images.isEmpty()) {
                ProductImage(null, product.name, Modifier.fillMaxWidth().height(360.dp))
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(product.images.sortedBy { it.sortOrder }, key = { it.id }) { image ->
                        ProductImage(image.url, image.altText ?: product.name, Modifier.width(320.dp).height(360.dp).clip(MaterialTheme.shapes.large))
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(product.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(formatMoney(selectedVariant?.price, selectedVariant?.currency ?: "VND"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                if (product.reviewCount > 0) Text("★ ${product.ratingAverage ?: "—"} · ${product.reviewCount} đánh giá · ${product.soldCount} đã bán", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Bán bởi ${product.shopName}", style = MaterialTheme.typography.labelLarge)
                HorizontalDivider()
                SectionTitle("Chọn phân loại")
                if (product.variants.none { it.status == "ACTIVE" }) {
                    InlineMessage("Sản phẩm hiện không có phân loại đang bán.", true)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(product.variants.filter { it.status == "ACTIVE" }, key = { it.id }) { variant ->
                            FilterChip(selected = selectedVariant?.id == variant.id, onClick = { onVariant(variant) }, label = { Text(variant.name) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Số lượng", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { onQuantity(quantity - 1) }, enabled = quantity > 1) { Icon(Icons.Outlined.Remove, "Giảm số lượng") }
                    Text(quantity.toString(), modifier = Modifier.width(36.dp), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onQuantity(quantity + 1) }, enabled = quantity < 99) { Icon(Icons.Outlined.Add, "Tăng số lượng") }
                }
                when (cartAction) {
                    is ActionState.Error -> InlineMessage(cartAction.message, true)
                    is ActionState.Success -> InlineMessage(cartAction.message, false)
                    else -> Unit
                }
                when (accountAction) {
                    is ActionState.Error -> InlineMessage(accountAction.message, true)
                    is ActionState.Success -> InlineMessage(accountAction.message, false)
                    else -> Unit
                }
                HorizontalDivider()
                SectionTitle("Mô tả")
                Text(product.description)
                if (product.attributes.isNotEmpty()) {
                    SectionTitle("Thông tin sản phẩm")
                    product.attributes.forEach { Text("${it.name}: ${it.value}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                HorizontalDivider()
                SectionTitle("Đánh giá từ người mua")
            }
        }
        when (reviews) {
            LoadState.Loading, LoadState.Idle -> item { LoadingState("Đang tải đánh giá…", Modifier.height(140.dp)) }
            is LoadState.Error -> item { ErrorState(reviews.message) }
            is LoadState.Success -> if (reviews.data.isEmpty()) {
                item { EmptyState("Chưa có đánh giá", "Người mua đã hoàn tất đơn có thể để lại đánh giá.") }
            } else {
                items(reviews.data, key = { it.id }) { review ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(review.reviewerName, fontWeight = FontWeight.SemiBold)
                        Text("★".repeat(review.rating), color = MaterialTheme.colorScheme.primary)
                        review.content?.let { Text(it) }
                        review.sellerReply?.let { Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) { Text("Phản hồi từ shop: $it", Modifier.padding(10.dp)) } }
                        Text(formatBusinessTime(review.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
