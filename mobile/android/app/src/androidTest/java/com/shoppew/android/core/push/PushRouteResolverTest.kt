package com.shoppew.android.core.push

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PushRouteResolverTest {
    private val resolver = PushRouteResolver()

    @Test
    fun notificationPayloadRoutesOnlyKnownResources() {
        assertEquals(AppRoute.Product("ao-cotton"), resolver.fromData(mapOf("productSlug" to "ao-cotton")))
        assertEquals(AppRoute.Order("order-1"), resolver.fromData(mapOf("type" to "ORDER", "resourceId" to "order-1")))
        assertEquals(AppRoute.Notifications, resolver.fromData(mapOf("type" to "SYSTEM")))
        assertNull(resolver.fromData(mapOf("deepLink" to "https://attacker.example/orders/1")))
    }

    @Test
    fun verifiedWebAndCustomDeepLinksResolveToTypedRoutes() {
        assertEquals(AppRoute.Product("ao-cotton"), resolver.fromUri(Uri.parse("https://shoppew.vn/product/ao-cotton")))
        assertEquals(AppRoute.Order("order-1"), resolver.fromUri(Uri.parse("shoppew://order/order-1")))
        assertEquals(AppRoute.Notifications, resolver.fromUri(Uri.parse("shoppew://notifications")))
    }
}
