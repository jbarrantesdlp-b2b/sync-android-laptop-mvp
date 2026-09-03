package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NetworkState {
    object Disconnected : NetworkState()
    data class Connected(val type: String) : NetworkState() // WiFi, Cellular, Ethernet
    object Connecting : NetworkState()
    object Unknown : NetworkState()
}

class ConnectivityObserver(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkState.value = getNetworkType()
        }

        override fun onLost(network: Network) {
            if (!isNetworkConnected()) {
                _networkState.value = NetworkState.Disconnected
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _networkState.value = getNetworkType()
        }
    }

    fun startObserving() {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            _networkState.value = getNetworkType()
        } catch (e: Exception) {
            // Fallback for older API levels
        }
    }

    fun stopObserving() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
        }
    }

    private fun getNetworkType(): NetworkState {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        
        return when {
            capabilities == null -> NetworkState.Disconnected
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                NetworkState.Connected("WiFi")
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                NetworkState.Connected("Cellular")
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                NetworkState.Connected("Ethernet")
            }
            else -> NetworkState.Connected("Unknown")
        }
    }

    private fun isNetworkConnected(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
    }
}
