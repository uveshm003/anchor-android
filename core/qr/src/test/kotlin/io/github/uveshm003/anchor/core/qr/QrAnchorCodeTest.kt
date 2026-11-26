package io.github.uveshm003.anchor.core.qr

import android.app.Application
import android.content.Intent
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrAnchorCodeTest {
    @Test
    fun payload_round_trips_and_rejects_other_codes() {
        val secret = QrAnchorCode.newSecret()
        assertThat(secret).hasLength(32)
        assertThat(QrAnchorCode.newSecret()).isNotEqualTo(secret)

        val payload = QrAnchorCode.encode(secret)
        assertThat(QrAnchorCode.decode(payload)).isEqualTo(secret)
        assertThat(QrAnchorCode.decode("https://example.com")).isNull()
        assertThat(QrAnchorCode.decode("anchor-qr:1:not*base64")).isNull()
        assertThat(QrAnchorCode.decode("anchor-qr:1:AAAA")).isNull()
    }

    @Test
    fun the_bitmap_decodes_back_to_the_payload() {
        val payload = QrAnchorCode.encode(QrAnchorCode.newSecret())
        val bitmap = QrAnchorCode.bitmap(payload, 400)
        val pixels = IntArray(400 * 400).also { bitmap.getPixels(it, 0, 400, 0, 0, 400, 400) }
        assertThat(pixels.toSet()).containsAtLeast(Color.BLACK, Color.WHITE)

        val source = RGBLuminanceSource(400, 400, pixels)
        val text = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        assertThat(text).isEqualTo(payload)
    }

    @Test
    fun export_shares_a_png_and_a_pdf_through_the_file_provider() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val chooser = QrExport(context).shareIntent("Desk", QrAnchorCode.encode(QrAnchorCode.newSecret()), "Scan me")
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        val uris = send.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)!!

        // Robolectric has no native PdfDocument, so only the PNG is checked here; the PDF is a device step.
        assertThat(uris.map { it.lastPathSegment }).contains("anchor-Desk.png")
        assertThat(uris.all { it.authority == context.packageName + QrExport.AUTHORITY_SUFFIX }).isTrue()
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }

    @Test
    fun the_analyzer_decodes_a_luminance_frame_and_ignores_noise() {
        val payload = QrAnchorCode.encode(QrAnchorCode.newSecret())
        val size = 480
        val bitmap = QrAnchorCode.bitmap(payload, size)
        val pixels = IntArray(size * size).also { bitmap.getPixels(it, 0, size, 0, 0, size, size) }
        val luminance = ByteArray(pixels.size) { i -> (pixels[i] and 0xFF).toByte() }

        assertThat(QrAnalyzer.decode(luminance, size, size, size)).isEqualTo(payload)
        assertThat(QrAnalyzer.decode(ByteArray(size * size) { 0x7F }, size, size, size)).isNull()
    }
}
