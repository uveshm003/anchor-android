package io.github.uveshm003.anchor.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.SecureRandom
import java.util.Base64

/**
 * A QR anchor's payload: a random 32-byte secret, app-prefixed. Only `AnchorHasher.hash(secret)` is stored;
 * the secret lives on the printed code (ADR 0008: a photo of it is accepted, like a cloned tag).
 */
object QrAnchorCode {
    private const val PREFIX = "anchor-qr:1:"
    private const val SECRET_BYTES = 32
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun newSecret(): ByteArray = ByteArray(SECRET_BYTES).also(SecureRandom()::nextBytes)

    fun encode(secret: ByteArray): String = PREFIX + encoder.encodeToString(secret)

    /** The secret, or null if [text] isn't an Anchor QR code. */
    fun decode(text: String): ByteArray? {
        if (!text.startsWith(PREFIX)) return null
        return runCatching { decoder.decode(text.removePrefix(PREFIX)) }.getOrNull()?.takeIf { it.size == SECRET_BYTES }
    }

    /** Black-on-white QR bitmap of [payload], [size] px square, with a quiet zone. */
    fun bitmap(payload: String, size: Int): Bitmap {
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
