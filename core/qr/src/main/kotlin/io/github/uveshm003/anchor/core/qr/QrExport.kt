package io.github.uveshm003.anchor.core.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Printable copies of a QR anchor, shared through the share sheet (FileProvider, app cache, no network). */
class QrExport @Inject constructor(@ApplicationContext private val context: Context) {
    /** A PNG and an A4 PDF of the code for [name]; returns a share-sheet intent for both. */
    fun shareIntent(name: String, payload: String, instructions: String): Intent {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val safe = name.filter { it.isLetterOrDigit() }.ifEmpty { "anchor" }
        val png = File(dir, "anchor-$safe.png").also { file ->
            file.outputStream().use {
                QrAnchorCode.bitmap(payload, PNG_SIZE).compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it)
            }
        }
        // The PDF is a convenience for printing; if the platform can't render it, the PNG is still shared.
        val pdf = runCatching {
            File(dir, "anchor-$safe.pdf").also { writePdf(it, name, payload, instructions) }
        }.getOrNull()
        val uris = ArrayList(listOfNotNull(png, pdf).map(::uri))
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("*/*")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun writePdf(file: File, name: String, payload: String, instructions: String) {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
        val canvas = page.canvas
        val title = Paint().apply {
            textSize = TITLE_SIZE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val body = Paint().apply {
            textSize = BODY_SIZE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(name, A4_WIDTH / 2f, TITLE_Y, title)
        val qr = QrAnchorCode.bitmap(payload, QR_PT)
        canvas.drawBitmap(qr, (A4_WIDTH - QR_PT) / 2f, QR_Y, null)
        canvas.drawText(instructions, A4_WIDTH / 2f, QR_Y + QR_PT + BODY_GAP, body)
        document.finishPage(page)
        file.outputStream().use(document::writeTo)
        document.close()
    }

    private fun uri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)

    companion object {
        const val AUTHORITY_SUFFIX = ".qrfiles"
        private const val DIR = "qr"
        private const val PNG_SIZE = 1024
        private const val PNG_QUALITY = 100
        private const val A4_WIDTH = 595
        private const val A4_HEIGHT = 842
        private const val QR_PT = 360
        private const val QR_Y = 160f
        private const val TITLE_Y = 110f
        private const val TITLE_SIZE = 28f
        private const val BODY_SIZE = 12f
        private const val BODY_GAP = 40f
    }
}
