package com.shoppew.android.core.push

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface AppRoute {
    data class Product(val slug: String) : AppRoute
    data class Order(val orderId: String) : AppRoute
    data object Notifications : AppRoute
}

@Singleton
class AppRouteDispatcher @Inject constructor() {
    private val channel = Channel<AppRoute>(capacity = Channel.BUFFERED)
    val routes: Flow<AppRoute> = channel.receiveAsFlow()

    fun dispatch(route: AppRoute) {
        channel.trySend(route)
    }
}

@Singleton
class PushRouteResolver @Inject constructor() {
    fun fromData(data: Map<String, String>): AppRoute? {
        data["deepLink"]?.takeIf(String::isNotBlank)?.let { raw ->
            fromUri(runCatching { Uri.parse(raw) }.getOrNull())?.let { return it }
        }
        data["productSlug"]?.trim()?.takeIf(String::isNotEmpty)?.let { return AppRoute.Product(it) }
        data["orderId"]?.trim()?.takeIf(String::isNotEmpty)?.let { return AppRoute.Order(it) }
        return when (data["type"]?.uppercase()) {
            "ORDER" -> data["resourceId"]?.trim()?.takeIf(String::isNotEmpty)?.let(AppRoute::Order)
            "PROMOTION", "SYSTEM", "CHAT", "PAYMENT" -> AppRoute.Notifications
            else -> null
        }
    }

    fun fromUri(uri: Uri?): AppRoute? {
        uri ?: return null
        val path = uri.pathSegments.filter(String::isNotBlank)
        return when {
            uri.scheme == APP_SCHEME && uri.host == "product" -> path.firstOrNull()?.let(AppRoute::Product)
            uri.scheme == APP_SCHEME && uri.host == "order" -> path.firstOrNull()?.let(AppRoute::Order)
            uri.scheme == APP_SCHEME && uri.host == "notifications" -> AppRoute.Notifications
            uri.scheme == "https" && uri.host == WEB_HOST && path.firstOrNull() == "product" ->
                path.getOrNull(1)?.let(AppRoute::Product)
            uri.scheme == "https" && uri.host == WEB_HOST && path.firstOrNull() == "account" && path.getOrNull(1) == "orders" ->
                path.getOrNull(2)?.let(AppRoute::Order)
            uri.scheme == "https" && uri.host == WEB_HOST && path.firstOrNull() == "account" && path.getOrNull(1) == "notifications" ->
                AppRoute.Notifications
            else -> null
        }
    }

    fun toUri(route: AppRoute): Uri = when (route) {
        is AppRoute.Product -> Uri.Builder().scheme(APP_SCHEME).authority("product").appendPath(route.slug).build()
        is AppRoute.Order -> Uri.Builder().scheme(APP_SCHEME).authority("order").appendPath(route.orderId).build()
        AppRoute.Notifications -> Uri.Builder().scheme(APP_SCHEME).authority("notifications").build()
    }

    private companion object {
        const val APP_SCHEME = "shoppew"
        const val WEB_HOST = "shoppew.vn"
    }
}
