package com.shoppew.android.ui.common

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {
    @Test
    fun `VND renders without fractional units`() {
        val formatted = formatMoney(BigDecimal("123456"), "VND")
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')

        assertTrue(formatted.contains("123.456"))
        assertFalse(formatted.contains(",00"))
        assertTrue(formatted.contains("₫"))
    }

    @Test
    fun `UTC instant is intentionally rendered in Ho Chi Minh business time`() {
        assertEquals("07:30, 11/08/2026", formatBusinessTime("2026-08-11T00:30:00Z"))
    }

    @Test
    fun `known order statuses have concise Vietnamese labels`() {
        assertEquals("Hoàn tất", orderStatusLabel("COMPLETED"))
        assertEquals("Đang giao", orderStatusLabel("SHIPPED"))
    }

    @Test
    fun `malformed timestamp remains visible instead of crashing the screen`() {
        assertEquals("not-an-instant", formatBusinessTime("not-an-instant"))
    }
}
