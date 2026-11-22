package app.nearwave.radio

import android.content.Context
import android.location.LocationManager
import android.os.Build
import app.nearwave.audio.AudioConfig
import app.nearwave.audio.VoicePlayer
import app.nearwave.audio.VoiceRecorder
import app.nearwave.core.Identity
import app.nearwave.core.Incoming
import app.nearwave.core.LogEntry
import app.nearwave.core.Member
import app.nearwave.core.Packet
import app.nearwave.core.PeerInfo
import app.nearwave.core.Problem
import app.nearwave.core.Protocol
import app.nearwave.core.Proximity
import app.nearwave.core.RadioState
import app.nearwave.service.Alerts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * The single source of truth for the app. The UI reads [state] and calls the action methods;
 * the foreground service calls [start] and [stop].
 */
class RadioController(private val context: Context) : LinkManager.Listener {

    val identity = Identity(context)
    val alerts = Alerts(context)

    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    /** Set by the activity; decides whether alerts also post a notification. */
    @Volatile var appVisible = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val link = LinkManager(context, this)
    private val beacon = BeaconManager(context) { id, rssi -> scope.launch { onSighting(id, rssi) } }
    private val player = VoicePlayer()
    private val audioSeq = AtomicInteger(0)
    private val recorder = VoiceRecorder { frame ->
        link.send(Packet.Audio(audioSeq.getAndIncrement() and 0xFFFF, frame))
    }
    private val logIds = AtomicLong(0)

    private class Peer(val id: String, var name: String) {
        var connected = false
        var rssi: Double? = null
        var lastSeenAt = 0L
        var talking = false
        var talkingSince = 0L
    }

    private val peers = ConcurrentHashMap<String, Peer>()
    private var ticker: Job? = null
    private var talkTimeout: Job? = null
    private var lastBluetoothOn = true

    // ---- Lifecycle -------------------------------------------------------------------------

    fun start() {
        if (_state.value.running) return
        val code = identity.channelCode ?: return
        val name = identity.displayName ?: return
        peers.clear()
        identity.knownPeers().forEach { (id, peerName) -> peers[id] = Peer(id, peerName) }

        val hash = Identity.channelHash(code)
        lastBluetoothOn = beacon.isBluetoothOn
        link.start(identity.memberId, name, hash)
        beacon.start(hash, identity.memberId)
        player.start()
        _state.update { it.copy(running = true, problem = currentProblem()) }
        publish()
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick(code, name)
            }
        }
    }

    fun stop() {
        if (!_state.value.running) return
        stopTalking()
        ticker?.cancel()
        link.stop()
        beacon.stop()
        player.stop()
        _state.update { it.copy(running = false, transmitting = false) }
        publish()
    }

    /** Called after changing name or channel. */
    fun restart() {
        stop()
        start()
    }

    fun setRadarVisible(visible: Boolean) = beacon.setFastScan(visible)

    // ---- Actions from the UI ---------------------------------------------------------------

    /** Returns false when nobody is connected, so the UI can explain why nothing happened. */
    fun startTalking(): Boolean {
        if (_state.value.transmitting) return true
        if (!link.send(Packet.TalkStart)) return false
        if (!recorder.start()) {
            link.send(Packet.TalkEnd)
            return false
        }
        _state.update { it.copy(transmitting = true) }
        talkTimeout = scope.launch {
            delay(AudioConfig.MAX_TALK_MS)
            stopTalking()
        }
        return true
    }

    fun stopTalking() {
        talkTimeout?.cancel()
        if (!_state.value.transmitting) return
        recorder.stop()
        link.send(Packet.TalkEnd)
        _state.update { it.copy(transmitting = false) }
    }

    fun buzz(): Boolean {
        if (!link.send(Packet.Buzz)) return false
        addLog(LogEntry(logIds.incrementAndGet(), "You", true, LogEntry.Kind.Buzz, "", System.currentTimeMillis()))
        return true
    }

    fun sendText(text: String): Boolean {
        val body = text.trim().take(Protocol.MAX_TEXT_LENGTH)
        if (body.isEmpty() || !link.send(Packet.Text(body))) return false
        addLog(LogEntry(logIds.incrementAndGet(), "You", true, LogEntry.Kind.Text, body, System.currentTimeMillis()))
        return true
    }

    fun dismissIncoming() = _state.update { it.copy(incoming = null) }

    // ---- Link events (main thread) ---------------------------------------------------------

    override fun onPeerConnected(peer: PeerInfo) {
        val p = peers.getOrPut(peer.id) { Peer(peer.id, peer.name) }
        p.name = peer.name
        p.connected = true
        identity.rememberPeer(peer.id, peer.name)
        alerts.tick()
        publish()
    }

    override fun onPeerDisconnected(memberId: String) {
        peers[memberId]?.apply {
            connected = false
            talking = false
        }
        publish()
    }

    override fun onPacket(memberId: String, packet: Packet) {
        val peer = peers[memberId] ?: return
        when (packet) {
            is Packet.Audio -> player.feed(packet.ulaw)
            Packet.TalkStart -> {
                peer.talking = true
                peer.talkingSince = System.currentTimeMillis()
                publish()
            }
            Packet.TalkEnd -> {
                peer.talking = false
                publish()
            }
            Packet.Buzz -> {
                alerts.buzz(peer.name, notify = !appVisible)
                receive(LogEntry(logIds.incrementAndGet(), peer.name, false, LogEntry.Kind.Buzz, "", System.currentTimeMillis()))
            }
            is Packet.Text -> {
                alerts.message(peer.name, packet.body, notify = !appVisible)
                receive(LogEntry(logIds.incrementAndGet(), peer.name, false, LogEntry.Kind.Text, packet.body, System.currentTimeMillis()))
            }
        }
    }

    override fun onLinkError(message: String) {
        _state.update { it.copy(problem = Problem.LinkFailed) }
    }

    // ---- Radar -----------------------------------------------------------------------------

    private fun onSighting(id: String, rssi: Int) {
        val peer = peers.getOrPut(id) { Peer(id, "Someone") }
        // Exponential smoothing: signal strength jumps around a lot from moment to moment.
        peer.rssi = peer.rssi?.let { it * 0.7 + rssi * 0.3 } ?: rssi.toDouble()
        peer.lastSeenAt = System.currentTimeMillis()
        publish()
    }

    private fun proximityOf(peer: Peer, now: Long): Proximity {
        val fresh = now - peer.lastSeenAt < SIGHTING_STALE_MS
        val rssi = peer.rssi
        return when {
            fresh && rssi != null && rssi >= NEAR_DBM -> Proximity.Near
            fresh && rssi != null && rssi >= MEDIUM_DBM -> Proximity.Medium
            fresh && rssi != null -> Proximity.Far
            peer.connected -> Proximity.Connected
            else -> Proximity.OutOfRange
        }
    }

    // ---- Internals -------------------------------------------------------------------------

    private fun tick(code: String, name: String) {
        val now = System.currentTimeMillis()
        // Recover from a TalkEnd that never arrived.
        peers.values.filter { it.talking && now - it.talkingSince > AudioConfig.MAX_TALK_MS + 5_000 }
            .forEach { it.talking = false }

        val bluetoothOn = beacon.isBluetoothOn
        if (bluetoothOn && !lastBluetoothOn) {
            val hash = Identity.channelHash(code)
            link.start(identity.memberId, name, hash)
            beacon.start(hash, identity.memberId)
        }
        lastBluetoothOn = bluetoothOn
        _state.update { s ->
            val problem = currentProblem() ?: s.problem?.takeIf { it == Problem.LinkFailed && s.connectedCount == 0 }
            s.copy(problem = problem)
        }
        publish()
    }

    private fun currentProblem(): Problem? {
        if (!beacon.isBluetoothOn) return Problem.BluetoothOff
        // Before Android 12, Bluetooth scanning only works with the Location switch on.
        if (Build.VERSION.SDK_INT < 31 && !isLocationOn()) return Problem.LocationOff
        return null
    }

    private fun isLocationOn(): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return true
        return if (Build.VERSION.SDK_INT >= 28) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        val members = peers.values
            .map { Member(it.id, it.name, it.connected, proximityOf(it, now), it.talking, it.lastSeenAt) }
            .sortedWith(compareBy({ it.proximity.ordinal }, { it.name.lowercase() }))
        val connected = members.count { it.connected }
        val previous = _state.value.connectedCount
        _state.update { it.copy(members = members) }
        if (connected != previous && _state.value.running) alerts.updateRadio(connected)
    }

    private fun receive(entry: LogEntry) {
        addLog(entry)
        _state.update { it.copy(incoming = Incoming(entry)) }
    }

    private fun addLog(entry: LogEntry) {
        _state.update { it.copy(log = (it.log + entry).takeLast(MAX_LOG)) }
    }

    companion object {
        private const val TICK_MS = 2_000L
        private const val SIGHTING_STALE_MS = 20_000L
        private const val MAX_LOG = 100

        // Rough bands, tuned for phones in a cabin. Adjust after real-world testing.
        private const val NEAR_DBM = -62.0
        private const val MEDIUM_DBM = -76.0
    }
}
