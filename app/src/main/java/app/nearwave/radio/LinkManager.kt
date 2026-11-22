package app.nearwave.radio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.nearwave.core.Packet
import app.nearwave.core.PeerInfo
import app.nearwave.core.Protocol
import app.nearwave.core.toHex
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.concurrent.ConcurrentHashMap

/**
 * Phone-to-phone links using Google Nearby Connections (Bluetooth + Wi-Fi, no internet).
 *
 * Every phone advertises and discovers at the same time. The service id contains the channel
 * fingerprint, so phones on other channels never see each other. To avoid two phones dialling
 * each other at once, the one with the smaller member id calls; the other waits a few seconds
 * and only calls if nothing has arrived.
 */
class LinkManager(context: Context, private val listener: Listener) {

    interface Listener {
        fun onPeerConnected(peer: PeerInfo)
        fun onPeerDisconnected(memberId: String)
        fun onPacket(memberId: String, packet: Packet)
        /** [what] is "advertise" or "discover"; [code] is a Nearby status code. */
        fun onLinkError(what: String, code: Int, detail: String)
    }

    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())

    private val connected = ConcurrentHashMap<String, PeerInfo>()  // endpointId -> peer
    private val initiating = ConcurrentHashMap<String, PeerInfo>() // endpointId -> peer
    private val discovered = ConcurrentHashMap<String, PeerInfo>() // endpointId -> peer

    private var me: PeerInfo? = null
    private var serviceId = ""
    private var running = false

    fun start(myId: String, myName: String, channelHash: ByteArray) {
        stop()
        me = PeerInfo(myId, myName)
        serviceId = "app.nearwave.ch." + channelHash.toHex()
        running = true

        val advertising = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(me!!.encode(), serviceId, lifecycle, advertising)
            .addOnFailureListener { reportUnlessAlready(it, "advertise") }

        val discovery = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(serviceId, discoveryCallback, discovery)
            .addOnFailureListener { reportUnlessAlready(it, "discover") }
    }

    fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        val dropped = connected.values.map { it.id }
        connected.clear()
        initiating.clear()
        discovered.clear()
        dropped.forEach(listener::onPeerDisconnected)
    }

    /** Sends to everyone on the channel, or to one person. Returns false if nobody is connected. */
    fun send(packet: Packet, toMemberId: String? = null): Boolean {
        val targets = connected.filterValues { toMemberId == null || it.id == toMemberId }.keys.toList()
        if (targets.isEmpty()) return false
        client.sendPayload(targets, Payload.fromBytes(Protocol.encode(packet)))
            .addOnFailureListener { Log.w(TAG, "send failed", it) }
        return true
    }

    private fun isConnectedTo(memberId: String) = connected.values.any { it.id == memberId }

    private fun dial(endpointId: String, peer: PeerInfo) {
        val self = me ?: return
        if (!running || isConnectedTo(peer.id) || initiating.containsKey(endpointId)) return
        initiating[endpointId] = peer
        client.requestConnection(self.encode(), endpointId, lifecycle)
            .addOnFailureListener {
                initiating.remove(endpointId)
                val code = (it as? ApiException)?.statusCode
                if (code != ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT) {
                    Log.w(TAG, "requestConnection failed ($code)", it)
                }
            }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val self = me ?: return
            val peer = PeerInfo.decode(info.endpointInfo) ?: return
            if (peer.id == self.id) return
            discovered[endpointId] = peer
            if (self.id < peer.id) {
                dial(endpointId, peer)
            } else {
                // The other phone should call us. If it hasn't after a few seconds, call it.
                main.postDelayed({ if (discovered.containsKey(endpointId)) dial(endpointId, peer) }, FALLBACK_DIAL_MS)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            discovered.remove(endpointId)
        }
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val peer = PeerInfo.decode(info.endpointInfo)
            if (peer == null || isConnectedTo(peer.id)) {
                client.rejectConnection(endpointId)
                return
            }
            // Only phones that know the channel code can reach this service id.
            initiating[endpointId] = peer
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val peer = initiating.remove(endpointId) ?: return
            if (result.status.statusCode != ConnectionsStatusCodes.STATUS_OK) return
            if (isConnectedTo(peer.id)) {
                client.disconnectFromEndpoint(endpointId) // duplicate link, keep the first one
                return
            }
            connected[endpointId] = peer
            listener.onPeerConnected(peer)
        }

        override fun onDisconnected(endpointId: String) {
            val peer = connected.remove(endpointId) ?: return
            listener.onPeerDisconnected(peer.id)
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val peer = connected[endpointId] ?: return
            val packet = payload.asBytes()?.let(Protocol::decode) ?: return
            listener.onPacket(peer.id, packet)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private fun reportUnlessAlready(error: Exception, what: String) {
        val code = (error as? ApiException)?.statusCode
        if (code == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING ||
            code == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING
        ) return
        Log.w(TAG, "$what failed ($code)", error)
        val name = code?.let { runCatching { ConnectionsStatusCodes.getStatusCodeString(it) }.getOrNull() }
        listener.onLinkError(what, code ?: -1, name ?: error.message ?: error.javaClass.simpleName)
    }

    companion object {
        private const val TAG = "LinkManager"
        private const val FALLBACK_DIAL_MS = 6_000L

        /** Many-to-many: every family phone can link to every other. */
        private val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    }
}
