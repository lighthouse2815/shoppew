package com.shoppew.android.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

enum class ConnectivityStatus {
    Checking,
    Online,
    Offline,
}

interface ConnectivityObserver {
    val status: StateFlow<ConnectivityStatus>
}

@Singleton
class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityObserver {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val status: StateFlow<ConnectivityStatus> = callbackFlow {
        fun emitCurrentStatus() {
            trySend(if (connectivityManager.hasInternetNetwork()) ConnectivityStatus.Online else ConnectivityStatus.Offline)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrentStatus()
            override fun onLost(network: Network) = emitCurrentStatus()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = emitCurrentStatus()
        }

        emitCurrentStatus()
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { runCatching { connectivityManager.unregisterNetworkCallback(callback) } }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = currentStatus())

    private fun currentStatus(): ConnectivityStatus =
        if (connectivityManager.hasInternetNetwork()) ConnectivityStatus.Online else ConnectivityStatus.Offline
}

private fun ConnectivityManager.hasInternetNetwork(): Boolean {
    val network = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(network) ?: return false
    // NET_CAPABILITY_INTERNET is intentional instead of VALIDATED: local development can reach
    // 10.0.2.2 on a network that Android has not validated against the public internet.
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
