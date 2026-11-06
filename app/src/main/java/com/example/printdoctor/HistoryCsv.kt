package com.example.printdoctor

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Convierte el historial en un CSV (texto con columnas separadas por comas) que se abre
 * directo en Excel o Google Sheets. Sirve para armar la tabla de resultados de la tesis:
 * lo que dijo la app contra lo que pasó en realidad.
 *
 * Va de la fila más vieja a la más nueva, y cada celda va entre comillas.
 */
fun buildHistoryCsv(entries: List<HistoryEntry>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    val header = listOf(
        "fecha", "foto", "filamento", "temp_boquilla_C", "temp_cama_C", "notas",
        "diagnostico_ia", "confianza_ia", "explicacion_ia", "consejos_ia",
        "acerto", "causa_real"
    )

    val rows = entries.sortedBy { it.createdAt }.map { entry ->
        listOf(
            dateFormat.format(Date(entry.createdAt)),
            File(entry.photoPath).name,
            entry.info.filament,
            entry.info.nozzleTemp,
            entry.info.bedTemp,
            entry.info.notes,
            entry.result.diagnosis,
            entry.result.confidence,
            entry.result.explanation,
            entry.result.advice.joinToString(" | "),
            entry.verdict?.csvLabel ?: "",
            entry.actualCause
        )
    }

    return (listOf(header) + rows).joinToString(separator = "\r\n", postfix = "\r\n") { row ->
        row.joinToString(",") { csvCell(it) }
    }
}

// Una celda entre comillas (las comillas internas se duplican, como pide el formato CSV)
private fun csvCell(value: String): String {
    var text = value.trim()
    // Si empieza con = + - @ , Excel podría tomarlo por una fórmula: se le antepone un apóstrofo
    if (text.isNotEmpty() && text[0] in "=+-@") text = "'$text"
    return "\"" + text.replace("\"", "\"\"") + "\""
}
