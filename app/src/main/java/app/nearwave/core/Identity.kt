package app.nearwave.core

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Who this phone is and which channel it is on. Stored locally; there is no account or server.
 *
 * A channel code like "K7QM-4XPA" is shared once (in person). Everyone with the same code
 * finds each other, and nobody else does.
 */
class Identity(context: Context) {

    private val prefs = context.getSharedPreferences("nearwave", Context.MODE_PRIVATE)

    /** Short random id for this install, 8 hex characters. */
    val memberId: String
        get() = prefs.getString(KEY_MEMBER_ID, null) ?: newMemberId().also {
            prefs.edit().putString(KEY_MEMBER_ID, it).apply()
        }

    var displayName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value?.let(::cleanName)).apply()

    var channelCode: String?
        get() = prefs.getString(KEY_CHANNEL, null)
        set(value) = prefs.edit().putString(KEY_CHANNEL, value).apply()

    /** When true, one tap starts talking and a second tap stops (easier for unsteady hands). */
    var tapToTalk: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_TALK, false)
        set(value) = prefs.edit().putBoolean(KEY_TAP_TO_TALK, value).apply()

    val isSetUp: Boolean
        get() = !displayName.isNullOrBlank() && !channelCode.isNullOrBlank()

    /** Names of people we have connected to before, so the radar can label them early. */
    fun knownPeers(): Map<String, String> =
        prefs.getString(KEY_PEERS, "").orEmpty()
            .split(';')
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size == 2 && isMemberId(parts[0])) parts[0] to parts[1] else null
            }
            .toMap()

    fun rememberPeer(id: String, name: String) {
        val peers = knownPeers().toMutableMap()
        if (peers[id] == name) return
        peers[id] = cleanName(name)
        prefs.edit().putString(KEY_PEERS, peers.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    fun leaveChannel() {
        prefs.edit().remove(KEY_CHANNEL).remove(KEY_PEERS).apply()
    }

    companion object {
        private const val KEY_MEMBER_ID = "member_id"
        private const val KEY_NAME = "name"
        private const val KEY_CHANNEL = "channel"
        private const val KEY_PEERS = "peers"
        private const val KEY_TAP_TO_TALK = "tap_to_talk"

        const val MAX_NAME_LENGTH = 20

        // No 0/O, 1/I/L: easy to read aloud and type.
        private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun newChannelCode(): String {
            val raw = (1..8).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
            return raw.substring(0, 4) + "-" + raw.substring(4)
        }

        /** Accepts "k7qm 4xpa", "K7QM-4XPA" etc. Returns the canonical form or null. */
        fun normalizeChannelCode(input: String): String? {
            val chars = input.uppercase().filter { it in CODE_ALPHABET }
            if (chars.length != 8) return null
            return chars.substring(0, 4) + "-" + chars.substring(4)
        }

        /** 8-byte fingerprint of the channel, used to scope discovery. */
        fun channelHash(code: String): ByteArray =
            MessageDigest.getInstance("SHA-256")
                .digest("nearwave-channel:$code".toByteArray(Charsets.UTF_8))
                .copyOf(8)

        fun cleanName(name: String): String =
            name.replace(Regex("[|;=\\n\\r]"), " ").trim().take(MAX_NAME_LENGTH)

        fun isMemberId(value: String): Boolean =
            value.length == 8 && value.all { it in '0'..'9' || it in 'a'..'f' }

        private fun newMemberId(): String {
            val bytes = ByteArray(4).also(random::nextBytes)
            return bytes.toHex()
        }
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }

fun String.hexToBytes(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()
