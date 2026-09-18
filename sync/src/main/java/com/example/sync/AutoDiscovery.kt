package com.example.sync

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoveredPeer(
    val name: String,
    val url: String,
    val via: String
)

/**
 * Finds a SYNC ENGINE laptop without QR.
 * Wi-Fi: UDP broadcast/multicast 41234 + mDNS `_syncengine._tcp`.
 * Bluetooth LE: manufacturer 0xFFFF payload `SE` + IPv4 + port.
 */
class AutoDiscovery(
    private val context: Context,
    private val onPeer: (DiscoveredPeer) -> Unit
) {
    companion object {
        const val UDP_PORT = 41234
        const val MAGIC = "SYNC_ENGINE"
        const val NSD_TYPE = "_syncengine._tcp."
        const val BLE_COMPANY = 0xFFFF
        private val MULTICAST = InetAddress.getByName("239.55.55.1")
    }

    private val _status = MutableStateFlow("IDLE")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _peer = MutableStateFlow<DiscoveredPeer?>(null)
    val peer: StateFlow<DiscoveredPeer?> = _peer.asStateFlow()

    private val running = AtomicBoolean(false)
    private var multicastLock: WifiManager.MulticastLock? = null
    private var udpThread: Thread? = null
    private var udpSocket: MulticastSocket? = null
    private var nsdManager: NsdManager? = null
    private var nsdListener: NsdManager.DiscoveryListener? = null
    private var bleCallback: ScanCallback? = null
    private val seen = LinkedHashMap<String, Long>()

    fun start() {
        if (!running.compareAndSet(false, true)) return
        _status.value = "Buscando laptop…"
        acquireMulticast()
        startUdp()
        startNsd()
        startBle()
    }

    fun stop() {
        running.set(false)
        _status.value = "IDLE"
        try { udpSocket?.close() } catch (_: Exception) {}
        udpThread = null
        nsdListener?.let { listener ->
            try { nsdManager?.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        nsdListener = null
        stopBle()
        try { multicastLock?.release() } catch (_: Exception) {}
        multicastLock = null
    }

    private fun emit(peer: DiscoveredPeer) {
        val url = SyncRepository.normalizeWsUrl(peer.url)
        if (url.isBlank()) return
        val now = System.currentTimeMillis()
        val last = seen[url]
        if (last != null && now - last < 4_000) return
        seen[url] = now
        val clean = peer.copy(url = url)
        _peer.value = clean
        _status.value = "Encontrada por ${clean.via}: ${clean.name}"
        onPeer(clean)
    }

    private fun acquireMulticast() {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifi?.createMulticastLock("sync-engine-mdns")?.apply {
            setReferenceCounted(true)
            acquire()
        }
    }

    private fun startUdp() {
        udpThread = Thread({
            try {
                val socket = MulticastSocket(UDP_PORT)
                socket.reuseAddress = true
                socket.soTimeout = 2000
                try { socket.joinGroup(MULTICAST) } catch (_: Exception) {}
                udpSocket = socket
                val buf = ByteArray(512)
                while (running.get()) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        parseBeacon(text, "Wi-Fi")
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }, "sync-udp-discovery").also { it.isDaemon = true; it.start() }
    }

    private fun parseBeacon(text: String, via: String) {
        try {
            val obj = JSONObject(text)
            if (obj.optString("app") != MAGIC) return
            val url = obj.optString("url").ifBlank {
                val ip = obj.optString("ip")
                val port = obj.optInt("port", 8123)
                if (ip.isBlank()) return else "ws://$ip:$port"
            }
            emit(DiscoveredPeer(obj.optString("name", "SYNC ENGINE"), url, via))
        } catch (_: Exception) {
        }
    }

    private fun startNsd() {
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = manager
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("syncengine")) return
                manager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val attrs = info.attributes
                        val urlBytes = attrs?.get("url")
                        val url = urlBytes?.toString(Charsets.UTF_8)
                            ?: info.host?.hostAddress?.let { "ws://$it:${info.port}" }
                            ?: return
                        emit(DiscoveredPeer(info.serviceName ?: "SYNC ENGINE", url, "mDNS"))
                    }
                })
            }
        }
        nsdListener = listener
        try {
            manager.discoverServices(NSD_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBle() {
        if (!hasBlePermission()) return
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setManufacturerData(BLE_COMPANY, byteArrayOf(0x53.toByte(), 0x45.toByte()), byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getManufacturerSpecificData(BLE_COMPANY) ?: return
                if (data.size < 8 || data[0] != 0x53.toByte() || data[1] != 0x45.toByte()) return
                val ip = "${data[2].toInt() and 0xFF}.${data[3].toInt() and 0xFF}.${data[4].toInt() and 0xFF}.${data[5].toInt() and 0xFF}"
                val port = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
                val name = result.scanRecord?.deviceName ?: result.device?.name ?: "SYNC ENGINE"
                emit(DiscoveredPeer(name, "ws://$ip:$port", "Bluetooth"))
            }
        }
        bleCallback = cb
        try {
            scanner.startScan(listOf(filter), settings, cb)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun stopBle() {
        val cb = bleCallback ?: return
        bleCallback = null
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            adapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (_: Exception) {
        }
    }
}
