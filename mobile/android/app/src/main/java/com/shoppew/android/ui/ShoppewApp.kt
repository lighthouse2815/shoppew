package com.shoppew.android.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shoppew.android.core.connectivity.ConnectivityStatus
import com.shoppew.android.core.push.AppRoute
import com.shoppew.android.ui.common.LoadingState
import com.shoppew.android.ui.common.NetworkStatusBanner
import com.shoppew.android.ui.screens.AccountScreen
import com.shoppew.android.ui.screens.AddressEditorScreen
import com.shoppew.android.ui.screens.AddressesScreen
import com.shoppew.android.ui.screens.AuthRequiredScreen
import com.shoppew.android.ui.screens.CartScreen
import com.shoppew.android.ui.screens.CatalogScreen
import com.shoppew.android.ui.screens.CheckoutScreen
import com.shoppew.android.ui.screens.LoginScreen
import com.shoppew.android.ui.screens.NotificationsScreen
import com.shoppew.android.ui.screens.OrderDetailScreen
import com.shoppew.android.ui.screens.OrdersScreen
import com.shoppew.android.ui.screens.ProductDetailScreen
import com.shoppew.android.ui.screens.ProfileScreen
import com.shoppew.android.ui.screens.RegisterScreen
import com.shoppew.android.ui.screens.ReviewsScreen
import com.shoppew.android.ui.screens.WishlistScreen
import com.shoppew.android.ui.state.AccountViewModel
import com.shoppew.android.ui.state.CartViewModel
import com.shoppew.android.ui.state.CatalogViewModel
import com.shoppew.android.ui.state.CheckoutViewModel
import com.shoppew.android.ui.state.ConnectivityViewModel
import com.shoppew.android.ui.state.OrdersViewModel
import com.shoppew.android.ui.state.SessionViewModel
import kotlinx.coroutines.flow.Flow

object Routes {
    const val Home = "home"
    const val Search = "search"
    const val Product = "product/{slug}"
    const val Login = "login"
    const val Register = "register"
    const val Cart = "cart"
    const val Checkout = "checkout"
    const val Orders = "orders"
    const val Order = "order/{orderId}"
    const val Account = "account"
    const val Profile = "profile"
    const val Addresses = "addresses"
    const val AddressEditor = "address-editor?addressId={addressId}"
    const val Wishlist = "wishlist"
    const val Notifications = "notifications"
    const val Reviews = "reviews"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ShoppewApp(routeEvents: Flow<AppRoute>) {
    val session: SessionViewModel = hiltViewModel()
    val catalog: CatalogViewModel = hiltViewModel()
    val cart: CartViewModel = hiltViewModel()
    val checkout: CheckoutViewModel = hiltViewModel()
    val orders: OrdersViewModel = hiltViewModel()
    val account: AccountViewModel = hiltViewModel()
    val connectivity: ConnectivityViewModel = hiltViewModel()
    val sessionState by session.uiState.collectAsStateWithLifecycle()
    val connectivityStatus by connectivity.status.collectAsStateWithLifecycle()
    if (sessionState.restoring) {
        LoadingState("Đang khôi phục phiên đăng nhập…")
        return
    }

    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    var pendingProtectedRoute by remember { mutableStateOf<AppRoute?>(null) }
    val latestUser by rememberUpdatedState(sessionState.user)

    fun navigateTo(route: AppRoute) {
        val destination = when (route) {
            is AppRoute.Product -> "product/${Uri.encode(route.slug)}"
            is AppRoute.Order -> "order/${Uri.encode(route.orderId)}"
            AppRoute.Notifications -> Routes.Notifications
        }
        navController.navigate(destination) { launchSingleTop = true }
    }

    LaunchedEffect(routeEvents) {
        routeEvents.collect { route ->
            val protected = route is AppRoute.Order || route is AppRoute.Notifications
            if (protected && latestUser == null) {
                pendingProtectedRoute = route
                navController.navigate(Routes.Login) { launchSingleTop = true }
            } else {
                navigateTo(route)
            }
        }
    }
    LaunchedEffect(sessionState.user?.id, pendingProtectedRoute) {
        val route = pendingProtectedRoute
        if (sessionState.user != null && route != null) {
            pendingProtectedRoute = null
            navigateTo(route)
        }
    }
    val bottoms = listOf(
        BottomDestination(Routes.Home, "Trang chủ", Icons.Outlined.Home),
        BottomDestination(Routes.Search, "Tìm kiếm", Icons.Outlined.Search),
        BottomDestination(Routes.Cart, "Giỏ hàng", Icons.Outlined.ShoppingCart),
        BottomDestination(Routes.Orders, "Đơn hàng", Icons.Outlined.ReceiptLong),
        BottomDestination(Routes.Account, "Tài khoản", Icons.Outlined.AccountCircle),
    )
    val showBottom = bottoms.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (connectivityStatus == ConnectivityStatus.Offline) {
                NetworkStatusBanner()
            }
        },
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    bottoms.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController, startDestination = Routes.Home) {
                composable(Routes.Home) {
                    CatalogScreen(catalog, homeMode = true, onProduct = { navController.navigate("product/${Uri.encode(it)}") }, onSearch = { navController.navigate(Routes.Search) })
                }
                composable(Routes.Search) {
                    CatalogScreen(catalog, homeMode = false, onProduct = { navController.navigate("product/${Uri.encode(it)}") })
                }
                composable(Routes.Product, arguments = listOf(navArgument("slug") { type = NavType.StringType })) { backStack ->
                    ProductDetailScreen(
                        slug = Uri.decode(backStack.arguments?.getString("slug").orEmpty()),
                        viewModel = catalog,
                        cartViewModel = cart,
                        accountViewModel = account,
                        authenticated = sessionState.user != null,
                        onBack = navController::popBackStack,
                        onLogin = { navController.navigate(Routes.Login) },
                        onCart = { navController.navigate(Routes.Cart) },
                    )
                }
                composable(Routes.Login) {
                    LoginScreen(session, onBack = navController::popBackStack, onRegister = { navController.navigate(Routes.Register) }, onSuccess = navController::popBackStack)
                }
                composable(Routes.Register) {
                    RegisterScreen(session, onBack = navController::popBackStack, onLogin = navController::popBackStack, onSuccess = { navController.popBackStack(Routes.Home, false) })
                }
                composable(Routes.Cart) {
                    if (sessionState.user == null) AuthRequiredScreen("Đăng nhập để xem giỏ hàng", { navController.navigate(Routes.Login) })
                    else CartScreen(cart, onProduct = { navController.navigate("product/${Uri.encode(it)}") }, onCheckout = { navController.navigate(Routes.Checkout) })
                }
                composable(Routes.Checkout) {
                    if (sessionState.user == null) AuthRequiredScreen("Đăng nhập để thanh toán", { navController.navigate(Routes.Login) })
                    else CheckoutScreen(
                        cartViewModel = cart,
                        viewModel = checkout,
                        onBack = navController::popBackStack,
                        onAddAddress = { navController.navigate("address-editor") },
                        onOrder = { orderId -> navController.navigate("order/$orderId") { popUpTo(Routes.Cart) { inclusive = true } } },
                    )
                }
                composable(Routes.Orders) {
                    if (sessionState.user == null) AuthRequiredScreen("Đăng nhập để xem đơn hàng", { navController.navigate(Routes.Login) })
                    else OrdersScreen(orders, onOrder = { navController.navigate("order/$it") })
                }
                composable(Routes.Order, arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { backStack ->
                    OrderDetailScreen(
                        orderId = backStack.arguments?.getString("orderId").orEmpty(),
                        viewModel = orders,
                        accountViewModel = account,
                        onBack = navController::popBackStack,
                    )
                }
                composable(Routes.Account) {
                    if (sessionState.user == null) AuthRequiredScreen("Đăng nhập để quản lý tài khoản", { navController.navigate(Routes.Login) })
                    else AccountScreen(
                        user = sessionState.user!!,
                        accountViewModel = account,
                        onProfile = { navController.navigate(Routes.Profile) },
                        onAddresses = { navController.navigate(Routes.Addresses) },
                        onWishlist = { navController.navigate(Routes.Wishlist) },
                        onNotifications = { navController.navigate(Routes.Notifications) },
                        onReviews = { navController.navigate(Routes.Reviews) },
                        onLogout = { session.logout { navController.navigate(Routes.Home) { popUpTo(0) } } },
                    )
                }
                composable(Routes.Profile) { ProfileScreen(account, navController::popBackStack) }
                composable(Routes.Addresses) {
                    AddressesScreen(account, navController::popBackStack, onCreate = { navController.navigate("address-editor") }, onEdit = { navController.navigate("address-editor?addressId=$it") })
                }
                composable(
                    Routes.AddressEditor,
                    arguments = listOf(navArgument("addressId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { backStack ->
                    AddressEditorScreen(backStack.arguments?.getString("addressId"), account, navController::popBackStack)
                }
                composable(Routes.Wishlist) { WishlistScreen(account, navController::popBackStack, onProduct = { navController.navigate("product/${Uri.encode(it)}") }) }
                composable(Routes.Notifications) { NotificationsScreen(account, navController::popBackStack) }
                composable(Routes.Reviews) { ReviewsScreen(account, navController::popBackStack) }
            }
        }
    }
}
