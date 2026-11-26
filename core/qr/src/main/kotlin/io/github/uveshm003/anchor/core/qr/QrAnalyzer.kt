package io.github.uveshm003.anchor.core.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** CameraX analyzer that decodes QR codes offline (ZXing) from the luminance plane of each frame. */
class QrAnalyzer(private val onText: (String) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = it.planes.first()
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining()).also(buffer::get)
            decode(bytes, plane.rowStride, it.width, it.height)?.let(onText)
        }
    }

    companion object {
        private val HINTS = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
        )

        /** The QR text in a luminance (Y) frame, or null if there's none. */
        fun decode(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
            val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
            return try {
                QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), HINTS).text
            } catch (_: NotFoundException) {
                null
            } catch (_: ChecksumException) {
                null
            } catch (_: FormatException) {
                null
            }
        }
    }
}
