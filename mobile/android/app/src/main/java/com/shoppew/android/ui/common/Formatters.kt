package com.shoppew.android.ui.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vietnameseLocale = Locale.forLanguageTag("vi-VN")
private val businessZone = ZoneId.of("Asia/Ho_Chi_Minh")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", vietnameseLocale)

fun formatMoney(amount: BigDecimal?, currency: String = "VND"): String {
    if (amount == null) return "—"
    val formatter = NumberFormat.getCurrencyInstance(vietnameseLocale)
    formatter.currency = java.util.Currency.getInstance(currency)
    formatter.maximumFractionDigits = if (currency == "VND") 0 else 2
    return formatter.format(amount)
}

fun formatBusinessTime(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return runCatching { Instant.parse(value).atZone(businessZone).format(dateTimeFormatter) }.getOrDefault(value)
}

fun orderStatusLabel(status: String): String = when (status) {
    "PENDING_PAYMENT" -> "Chờ thanh toán"
    "PAID" -> "Đã thanh toán"
    "CONFIRMED" -> "Đã xác nhận"
    "PROCESSING" -> "Đang chuẩn bị"
    "READY_TO_SHIP" -> "Sẵn sàng giao"
    "SHIPPED" -> "Đang giao"
    "DELIVERED" -> "Đã giao"
    "COMPLETED" -> "Hoàn tất"
    "CANCELLED" -> "Đã huỷ"
    "REFUND_REQUESTED" -> "Đang hoàn tiền"
    "PARTIALLY_REFUNDED" -> "Hoàn tiền một phần"
    "REFUNDED" -> "Đã hoàn tiền"
    else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
