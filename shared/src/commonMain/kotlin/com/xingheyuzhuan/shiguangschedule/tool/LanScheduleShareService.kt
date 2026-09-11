package com.xingheyuzhuan.shiguangschedule.tool

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import org.koin.core.annotation.Single
import kotlin.random.Random

data class LanSharePeer(val name: String, val address: String)

expect fun platformDeviceName(): String
expect fun platformBroadcastAddresses(): List<String>

/** Lightweight discovery and transfer protocol used only by Shiguang Schedule clients. */
@Single
class LanScheduleShareService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selector = SelectorManager(Dispatchers.Default)
    private val _peers = MutableStateFlow<List<LanSharePeer>>(emptyList())
    val peers = _peers.asStateFlow()

    private var serverJob: Job? = null
    private var discoveryJob: Job? = null
    private var tcpServer: ServerSocket? = null
    private var discoverySocket: BoundDatagramSocket? = null
    private var onScheduleReceived: ((String) -> Unit)? = null
    private val instanceId = Random.nextLong().toString()
    private val deviceName = platformDeviceName().ifBlank { DEFAULT_DEVICE_NAME }.take(40)

    fun start(onReceived: (String) -> Unit) {
        onScheduleReceived = onReceived
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                tcpServer = aSocket(selector).tcp().bind("0.0.0.0", TRANSFER_PORT)
                while (isActive) {
                    val socket = tcpServer?.accept() ?: break
                    launch {
                        try {
                            val input = socket.openReadChannel()
                            val size = input.readInt()
                            require(size in 1..MAX_SCHEDULE_BYTES)
                            onScheduleReceived?.invoke(input.readByteArray(size).decodeToString())
                        } catch (_: Exception) {
                            // Ignore malformed or interrupted incoming transfers.
                        } finally {
                            socket.close()
                        }
                    }
                }
            } catch (error: Exception) {
                println("LAN transfer server failed: ${error::class.simpleName}: ${error.message.orEmpty()}")
                tcpServer?.close()
                tcpServer = null
            }
        }

        discoveryJob = scope.launch {
            try {
                val socket = aSocket(selector).udp().bind("0.0.0.0", DISCOVERY_PORT) {
                    broadcast = true
                    reuseAddress = true
                }
                discoverySocket = socket
                sendDiscovery(socket)
                while (isActive) {
                    val datagram = socket.receive()
                    val message = datagram.packet.readString()
                    val remote = datagram.address as? InetSocketAddress ?: continue
                    when {
                        message.startsWith("$DISCOVER|") -> {
                            val parts = message.split('|', limit = 3)
                            if (parts.size != 3 || parts[1] == instanceId) continue
                            addPeer(parts[2], remote.hostname)
                            try {
                                socket.send(
                                    Datagram(Buffer().apply { writeString("$RESPONSE|$instanceId|$deviceName") }, remote)
                                )
                            } catch (error: Exception) {
                                println("LAN discovery response failed: ${error.message}")
                            }
                        }
                        message.startsWith("$RESPONSE|") -> {
                            val parts = message.split('|', limit = 3)
                            if (parts.size != 3 || parts[1] == instanceId) continue
                            addPeer(parts[2], remote.hostname)
                        }
                    }
                }
            } catch (_: Exception) {
                // Permission denial and unsupported network conditions must not crash the app.
            } finally {
                discoverySocket?.close()
                discoverySocket = null
            }
        }
    }

    fun refresh() {
        _peers.value = emptyList()
        scope.launch {
            try {
                discoverySocket?.let { sendDiscovery(it) }
            } catch (_: Exception) {
                // Keep the dialog usable when broadcast is blocked by the current network.
            }
        }
    }

    private suspend fun sendDiscovery(socket: BoundDatagramSocket) {
        val message = "$DISCOVER|$instanceId|$deviceName"
        var sent = false
        val addresses = platformBroadcastAddresses()
        println("LAN discovery broadcast addresses: ${addresses.joinToString()}")
        addresses.forEach { address ->
            try {
                socket.send(
                    Datagram(
                        Buffer().apply { writeString(message) },
                        InetSocketAddress(address, DISCOVERY_PORT)
                    )
                )
                sent = true
            } catch (error: Exception) {
                println("LAN discovery send to $address failed: ${error.message}")
            }
        }
        if (!sent) {
            println("LAN discovery could not send on any network interface")
        }
    }

    private fun addPeer(name: String, address: String) {
        val peer = LanSharePeer(name.take(40), address)
        _peers.value = (_peers.value.filterNot { it.address == address } + peer).sortedBy { it.name }
    }

    suspend fun send(peer: LanSharePeer, json: String) = withContext(Dispatchers.Default) {
        val bytes = json.encodeToByteArray()
        require(bytes.size <= MAX_SCHEDULE_BYTES)
        val socket = aSocket(selector).tcp().connect(peer.address, TRANSFER_PORT)
        try {
            val output = socket.openWriteChannel(autoFlush = true)
            output.writeInt(bytes.size)
            output.writeFully(bytes)
            output.flush()
        } finally {
            socket.close()
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        serverJob?.cancel()
        discoverySocket?.close()
        tcpServer?.close()
        discoveryJob = null
        serverJob = null
        tcpServer = null
        discoverySocket = null
        onScheduleReceived = null
        _peers.value = emptyList()
    }

    companion object {
        private const val DEFAULT_DEVICE_NAME = "Shiguang Schedule"
        private const val DISCOVERY_PORT = 53318
        private const val TRANSFER_PORT = 53319
        private const val MAX_SCHEDULE_BYTES = 2 * 1024 * 1024
        private const val DISCOVER = "SGS_DISCOVER_V1"
        private const val RESPONSE = "SGS_HERE_V1"
    }
}