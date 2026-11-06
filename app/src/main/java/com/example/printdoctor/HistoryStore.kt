package com.example.printdoctor

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** ¿Acertó la IA? Lo marca el usuario cuando ya sabe qué pasó en realidad. */
enum class Verdict(val label: String, val badge: String, val csvLabel: String) {
    Yes("Sí", "✓ Acertó", "sí"),
    Partial("En parte", "≈ En parte", "en parte"),
    No("No", "✗ No acertó", "no")
}

/**
 * Un diagnóstico guardado: la foto, lo que escribiste en el formulario
 * y lo que respondió la IA. La ruta de la foto sirve como identificador
 * (cada foto tiene un nombre único con su marca de tiempo).
 *
 * verdict y actualCause son la retroalimentación del usuario (qué tan bien acertó la IA
 * y cuál fue la causa real); sirven para armar la tabla de resultados de la tesis.
 */
data class HistoryEntry(
    val photoPath: String,
    val createdAt: Long,        // milisegundos desde 1970 (cuándo se diagnosticó por primera vez)
    val info: PrintInfo,
    val result: DiagnosisResult,
    val verdict: Verdict? = null,
    val actualCause: String = ""
)

/**
 * Guarda y lee el historial en un archivo JSON dentro del almacenamiento privado de la app
 * (history.json). Nadie más puede leerlo, y se borra solo si desinstalas la app.
 *
 * `entries` es un "estado" de Compose: cuando cambia, las pantallas que lo leen
 * se vuelven a dibujar solas. Por eso no hay que avisarle nada a la interfaz.
 */
class HistoryStore(context: Context) {

    private val file = File(context.filesDir, "history.json")

    // Del más nuevo al más viejo
    var entries: List<HistoryEntry> by mutableStateOf(readFromDisk())
        private set

    /**
     * Guarda un diagnóstico. Si esa foto ya estaba en el historial (por ejemplo, porque
     * respondiste las preguntas de seguimiento y la IA afinó su respuesta), lo reemplaza
     * y conserva la fecha original.
     */
    fun save(photoPath: String, info: PrintInfo, result: DiagnosisResult) {
        val previous = entries.firstOrNull { it.photoPath == photoPath }
        val entry = HistoryEntry(
            photoPath = photoPath,
            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
            info = info,
            result = result,
            // La causa real es un dato de la impresión y se conserva siempre. La calificación
            // solo se conserva si el diagnóstico sigue siendo el mismo (si la IA lo cambió
            // tras las preguntas de seguimiento, ya hay que calificarlo de nuevo).
            verdict = previous?.takeIf { it.result.diagnosis == result.diagnosis }?.verdict,
            actualCause = previous?.actualCause ?: ""
        )
        entries = (listOf(entry) + entries.filter { it.photoPath != photoPath })
            .sortedByDescending { it.createdAt }
        writeToDisk()
    }

    /** Guarda la calificación ("¿acertó?") y la causa real de un diagnóstico. */
    fun updateFeedback(photoPath: String, verdict: Verdict?, actualCause: String) {
        entries = entries.map {
            if (it.photoPath == photoPath) it.copy(verdict = verdict, actualCause = actualCause) else it
        }
        writeToDisk()
    }

    /** Borra el diagnóstico y también su foto, para no ocupar espacio de más. */
    fun delete(entry: HistoryEntry) {
        entries = entries.filter { it.photoPath != entry.photoPath }
        writeToDisk()
        runCatching { File(entry.photoPath).delete() }
    }

    // ---------------------------------------------------------------------------------
    // Lectura y escritura del archivo
    // ---------------------------------------------------------------------------------

    private fun readFromDisk(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length())
                .mapNotNull { i -> runCatching { array.getJSONObject(i).toEntry() }.getOrNull() }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("PrintDoctor", "No se pudo leer el historial", e)
            emptyList()
        }
    }

    private fun writeToDisk() {
        try {
            val array = JSONArray()
            entries.forEach { array.put(it.toJson()) }
            val text = array.toString()

            // Escribimos primero a un archivo temporal y luego lo renombramos:
            // así, si la app se cierra a medio guardado, el historial anterior no se corrompe.
            val temp = File(file.parentFile, "history.json.tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.writeText(text)
                temp.delete()
            }
        } catch (e: Exception) {
            Log.e("PrintDoctor", "No se pudo guardar el historial", e)
        }
    }
}

// ---------------------------------------------------------------------------------
// Conversión entre HistoryEntry y JSON
// ---------------------------------------------------------------------------------

private fun HistoryEntry.toJson(): JSONObject = JSONObject().apply {
    put("photoPath", photoPath)
    put("createdAt", createdAt)
    put("filament", info.filament)
    put("nozzleTemp", info.nozzleTemp)
    put("bedTemp", info.bedTemp)
    put("notes", info.notes)
    put("diagnosis", result.diagnosis)
    put("confidence", result.confidence)
    put("explanation", result.explanation)
    put("advice", JSONArray(result.advice))
    put("followUpQuestions", JSONArray(result.followUpQuestions))
    put("verdict", verdict?.name ?: "")
    put("actualCause", actualCause)
}

private fun JSONObject.toEntry(): HistoryEntry = HistoryEntry(
    photoPath = getString("photoPath"),
    createdAt = getLong("createdAt"),
    info = PrintInfo(
        filament = optString("filament"),
        nozzleTemp = optString("nozzleTemp"),
        bedTemp = optString("bedTemp"),
        notes = optString("notes")
    ),
    result = DiagnosisResult(
        diagnosis = optString("diagnosis"),
        confidence = optString("confidence"),
        explanation = optString("explanation"),
        advice = optJSONArray("advice").toStringList(),
        followUpQuestions = optJSONArray("followUpQuestions").toStringList()
    ),
    // Los diagnósticos guardados antes de esta versión no traen estos campos: quedan vacíos
    verdict = optString("verdict").let { saved -> Verdict.entries.firstOrNull { it.name == saved } },
    actualCause = optString("actualCause")
)

private fun JSONArray?.toStringList(): List<String> {
    val array = this ?: return emptyList()
    return List(array.length()) { i -> array.getString(i) }
}
