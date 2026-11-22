package app.nearwave.core

/** Rough distance band from Bluetooth signal strength. Direction is not knowable. */
enum class Proximity { Near, Medium, Far, Connected, OutOfRange }

data class Member(
    val id: String,
    val name: String,
    val connected: Boolean,
    val proximity: Proximity,
    val talking: Boolean,
    val lastSeenAt: Long,
)

data class LogEntry(
    val id: Long,
    val fromName: String,
    val mine: Boolean,
    val kind: Kind,
    val body: String,
    val at: Long,
) {
    enum class Kind { Buzz, Text }
}

/** A buzz or message that just arrived, shown as a banner until dismissed. */
data class Incoming(val entry: LogEntry)

enum class Problem { BluetoothOff, LocationOff, LinkFailed }

data class RadioState(
    val running: Boolean = false,
    val members: List<Member> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val transmitting: Boolean = false,
    val incoming: Incoming? = null,
    val problem: Problem? = null,
    /** Technical detail for LinkFailed, e.g. "8034 MISSING_PERMISSION_ACCESS_COARSE_LOCATION". */
    val problemDetail: String? = null,
) {
    val connectedCount: Int get() = members.count { it.connected }
    val someoneTalking: Member? get() = members.firstOrNull { it.talking }
}
