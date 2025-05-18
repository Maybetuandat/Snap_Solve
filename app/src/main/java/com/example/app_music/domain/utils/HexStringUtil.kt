package com.example.app_music.domain.utils
import java.util.*
object HexStringUtil {
    private val HEX_CHAR_TABLE = byteArrayOf(
        '0'.toByte(), '1'.toByte(), '2'.toByte(), '3'.toByte(),
        '4'.toByte(), '5'.toByte(), '6'.toByte(), '7'.toByte(),
        '8'.toByte(), '9'.toByte(), 'a'.toByte(), 'b'.toByte(),
        'c'.toByte(), 'd'.toByte(), 'e'.toByte(), 'f'.toByte()
    )

    fun byteArrayToHexString(raw: ByteArray): String {
        val hex = ByteArray(2 * raw.size)
        var index = 0

        for (b in raw) {
            val v = b.toInt() and 0xFF
            hex[index++] = HEX_CHAR_TABLE[v ushr 4]
            hex[index++] = HEX_CHAR_TABLE[v and 0xF]
        }
        return String(hex)
    }

    fun hexStringToByteArray(hex: String): ByteArray {
        val hexStandard = hex.lowercase(Locale.ENGLISH)
        val sz = hexStandard.length / 2
        val bytesResult = ByteArray(sz)

        var idx = 0
        for (i in 0 until sz) {
            bytesResult[i] = hexStandard[idx].toByte()
            idx++
            var tmp = hexStandard[idx].toByte()
            idx++

            bytesResult[i] = if (bytesResult[i] > HEX_CHAR_TABLE[9]) {
                (bytesResult[i] - ('a'.toByte() - 10)).toByte()
            } else {
                (bytesResult[i] - '0'.toByte()).toByte()
            }

            tmp = if (tmp > HEX_CHAR_TABLE[9]) {
                (tmp - ('a'.toByte() - 10)).toByte()
            } else {
                (tmp - '0'.toByte()).toByte()
            }

            bytesResult[i] = (bytesResult[i] * 16 + tmp).toByte()
        }
        return bytesResult
    }
}