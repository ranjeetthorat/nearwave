package app.nearwave.core

/**
 * Everything sent between phones is one small packet: [type byte][payload].
 * Kept deliberately tiny so it is easy to port to another transport later (e.g. BLE for iPhone).
 */
sealed interface Packet {
    data object Buzz : Packet
    data class Text(val body: String) : Packet
    data object TalkStart : Packet
    class Audio(val seq: Int, val ulaw: ByteArray) : Packet
    data object TalkEnd : Packet
}

object Protocol {
    private const val BUZZ: Byte = 1
    private const val TEXT: Byte = 2
    private const val TALK_START: Byte = 3
    private const val AUDIO: Byte = 4
    private const val TALK_END: Byte = 5

    const val MAX_TEXT_LENGTH = 300

    fun encode(packet: Packet): ByteArray = when (packet) {
        Packet.Buzz -> byteArrayOf(BUZZ)
        is Packet.Text -> byteArrayOf(TEXT) + packet.body.take(MAX_TEXT_LENGTH).toByteArray(Charsets.UTF_8)
        Packet.TalkStart -> byteArrayOf(TALK_START)
        is Packet.Audio -> byteArrayOf(
            AUDIO,
            (packet.seq shr 8 and 0xFF).toByte(),
            (packet.seq and 0xFF).toByte(),
        ) + packet.ulaw
        Packet.TalkEnd -> byteArrayOf(TALK_END)
    }

    fun decode(bytes: ByteArray): Packet? {
        if (bytes.isEmpty()) return null
        return when (bytes[0]) {
            BUZZ -> Packet.Buzz
            TEXT -> Packet.Text(String(bytes, 1, bytes.size - 1, Charsets.UTF_8))
            TALK_START -> Packet.TalkStart
            AUDIO -> if (bytes.size > 3) {
                val seq = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                Packet.Audio(seq, bytes.copyOfRange(3, bytes.size))
            } else null
            TALK_END -> Packet.TalkEnd
            else -> null
        }
    }
}

/** What a phone announces about itself while looking for others: "nw1|<memberId>|<name>". */
data class PeerInfo(val id: String, val name: String) {
    fun encode(): ByteArray = "$VERSION|$id|$name".toByteArray(Charsets.UTF_8)

    companion object {
        private const val VERSION = "nw1"

        fun decode(bytes: ByteArray?): PeerInfo? {
            if (bytes == null) return null
            val parts = String(bytes, Charsets.UTF_8).split('|', limit = 3)
            if (parts.size != 3 || parts[0] != VERSION || !Identity.isMemberId(parts[1])) return null
            return PeerInfo(parts[1], Identity.cleanName(parts[2]).ifBlank { "Someone" })
        }
    }
}
