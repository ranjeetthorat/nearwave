package app.nearwave.audio

/** ITU-T G.711 mu-law: 16-bit PCM <-> 8-bit, the classic telephone voice codec. */
object MuLaw {
    private const val BIAS = 0x84
    private const val CLIP = 32635

    fun encode(pcm: ShortArray, count: Int = pcm.size): ByteArray =
        ByteArray(count) { encodeSample(pcm[it].toInt()) }

    fun decode(ulaw: ByteArray): ShortArray =
        ShortArray(ulaw.size) { decodeSample(ulaw[it]) }

    private fun encodeSample(input: Int): Byte {
        var sample = input
        val sign = if (sample < 0) {
            sample = -sample
            0x80
        } else 0
        if (sample > CLIP) sample = CLIP
        sample += BIAS
        var exponent = 7
        var mask = 0x4000
        while (exponent > 0 && (sample and mask) == 0) {
            exponent--
            mask = mask shr 1
        }
        val mantissa = (sample shr (exponent + 3)) and 0x0F
        return (sign or (exponent shl 4) or mantissa).inv().toByte()
    }

    private fun decodeSample(byte: Byte): Short {
        val u = byte.toInt().inv() and 0xFF
        val sign = u and 0x80
        val exponent = (u shr 4) and 0x07
        val mantissa = u and 0x0F
        val magnitude = (((mantissa shl 3) + BIAS) shl exponent) - BIAS
        return (if (sign != 0) -magnitude else magnitude).toShort()
    }
}
