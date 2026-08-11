package com.shoppew.android.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.shoppew.android.BuildConfig
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.ui.theme.Brand100
import com.shoppew.android.ui.theme.Brand950
import com.shoppew.android.ui.theme.Line
import java.net.URI

@Composable
fun LoadingState(label: String = "Đang tải dữ liệu…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        Text("Không tải được dữ liệu", style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) OutlinedButton(onClick = onRetry, modifier = Modifier.height(48.dp)) { Text("Thử lại") }
    }
}

@Composable
fun EmptyState(title: String, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Inbox, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) Button(onClick = onAction, modifier = Modifier.height(48.dp)) { Text(actionLabel) }
    }
}

@Composable
fun InlineMessage(message: String, isError: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else Brand100,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else Brand950,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NetworkStatusBanner(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = "Đang ngoại tuyến; nội dung sản phẩm đã lưu vẫn có thể xem"
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Đang ngoại tuyến · hiển thị dữ liệu sản phẩm đã lưu", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ProductImage(url: String?, description: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = resolveMediaUrl(url),
        contentDescription = description,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.BrokenImage, contentDescription = "Ảnh không khả dụng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

private fun resolveMediaUrl(url: String?): String? {
    if (!BuildConfig.DEBUG || url.isNullOrBlank()) return url
    val debugHost = runCatching { URI(BuildConfig.API_BASE_URL).host }
        .getOrNull()
        ?.takeIf(String::isNotBlank)
        ?: "10.0.2.2"
    return url
        .replace("://localhost", "://$debugHost")
        .replace("://127.0.0.1", "://$debugHost")
}

@Composable
fun ProductCard(product: ProductSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick).semantics { contentDescription = "Mở ${product.name}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Line),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        ProductImage(product.primaryImageUrl, product.name, Modifier.fillMaxWidth().height(152.dp).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(product.name, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2, style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(product.minimumPrice, product.currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(product.shopName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (product.reviewCount > 0) {
                Text("★ ${product.ratingAverage ?: "—"} · ${product.reviewCount} đánh giá", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            OutlinedButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(44.dp)) { Text(action) }
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(132.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .65f))
}
