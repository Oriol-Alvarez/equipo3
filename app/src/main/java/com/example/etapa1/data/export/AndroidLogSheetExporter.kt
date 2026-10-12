package com.example.etapa1.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.etapa1.domain.LogSheet
import com.example.etapa1.domain.LogSheetLayout
import com.example.etapa1.domain.SheetSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Genera el PDF con [PdfDocument] (incluido en Android, sin librerías extra) y lo guarda
 * en la caché de la app; de ahí se comparte por FileProvider o se copia a Descargas.
 */
class AndroidLogSheetExporter(private val context: Context) : LogSheetExporter {

    override val canSaveToDownloads: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override suspend fun exportPdf(
        sheet: LogSheet,
        fileName: String,
        generatedAt: String
    ): Result<ExportedFile> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "bitacoras").apply { mkdirs() }
            val file = File(directory, fileName)
            val document = PdfDocument()
            val pages = try {
                val surface = PdfSurface(document)
                val total = LogSheetLayout.draw(sheet, surface) { page, totalPages ->
                    "Generado con la app Sonrisas de Cristal el $generatedAt · Página $page de $totalPages"
                }
                FileOutputStream(file).use { document.writeTo(it) }
                total
            } finally {
                document.close()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ExportedFile(uri = uri.toString(), path = file.absolutePath, fileName = fileName, pages = pages)
        }
    }

    override suspend fun saveToDownloads(file: ExportedFile): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                error("Guardar en Descargas está disponible desde Android 10. Usa Compartir para guardarlo.")
            }
            copyToDownloads(file)
            "Guardado en Descargas como ${file.fileName}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyToDownloads(file: ExportedFile) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas.")
        try {
            val output = resolver.openOutputStream(target) ?: error("No se pudo escribir en Descargas.")
            output.use { out -> File(file.path).inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(target, values, null, null)
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            throw e
        }
    }

    /** Dibuja en las páginas del PDF; las medidas de PdfDocument ya están en puntos. */
    private class PdfSurface(private val document: PdfDocument) : SheetSurface {
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        private val canvas: Canvas
            get() = checkNotNull(page) { "No hay una página abierta" }.canvas

        private fun configure(size: Float, bold: Boolean) {
            textPaint.textSize = size
            textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        override fun measure(text: String, size: Float, bold: Boolean): Float {
            configure(size, bold)
            return textPaint.measureText(text)
        }

        override fun beginPage() {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(
                LogSheetLayout.PAGE_WIDTH.toInt(),
                LogSheetLayout.PAGE_HEIGHT.toInt(),
                pageNumber
            ).create()
            page = document.startPage(info)
        }

        override fun endPage() {
            page?.let { document.finishPage(it) }
            page = null
        }

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Int?, strokeColor: Int?) {
            fillColor?.let {
                fillPaint.color = it
                canvas.drawRect(left, top, right, bottom, fillPaint)
            }
            strokeColor?.let {
                strokePaint.color = it
                canvas.drawRect(left, top, right, bottom, strokePaint)
            }
        }

        override fun drawText(text: String, x: Float, baseline: Float, size: Float, bold: Boolean, color: Int) {
            if (text.isEmpty()) return
            configure(size, bold)
            textPaint.color = color
            canvas.drawText(text, x, baseline, textPaint)
        }
    }
}
