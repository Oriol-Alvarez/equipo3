package com.example.etapa1.data.export

import com.example.etapa1.domain.LogSheet

/** Archivo PDF generado; [uri] es un content:// que se puede abrir o compartir con otras apps. */
data class ExportedFile(
    val uri: String,
    val path: String,
    val fileName: String,
    val pages: Int
)

/** Convierte la hoja en un archivo PDF y lo guarda donde el usuario lo pueda usar. */
interface LogSheetExporter {
    /** Guardar en la carpeta Descargas solo está disponible desde Android 10. */
    val canSaveToDownloads: Boolean

    suspend fun exportPdf(sheet: LogSheet, fileName: String, generatedAt: String): Result<ExportedFile>

    /** Copia el PDF a Descargas y devuelve un mensaje para mostrar al usuario. */
    suspend fun saveToDownloads(file: ExportedFile): Result<String>
}
