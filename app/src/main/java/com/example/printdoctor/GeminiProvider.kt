package com.example.printdoctor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Conexión con la API de Gemini usando solo herramientas que ya trae Android
 * (sin librerías extra).
 *
 * Los nombres de modelo cambian seguido: si algún día da error 404,
 * copia el nombre vigente desde Google AI Studio.
 *  - "gemini-3.5-flash-lite": más solicitudes gratis por día, ideal para desarrollar.
 *  - "gemini-3.8-flash": más capaz, pero con menos solicitudes gratis; úsalo para
 *    las pruebas de la tesis.
 */
class GeminiProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = "gemini-3.5-flash-lite"
) : DiagnosisProvider {

    override suspend fun diagnose(
        photoPath: String,
        info: PrintInfo,
        extraAnswers: String?
    ): DiagnosisResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "Falta la clave de Gemini: agrega GEMINI_API_KEY en local.properties"
        }
        val imageBase64 = jpegBase64(photoPath)
        val requestBody = buildRequestBody(imageBase64, info, extraAnswers)
        val responseText = post(requestBody)
        parseResult(responseText)
    }

    // ---------- 1) Armar la petición ----------

    private fun buildRequestBody(
        imageBase64: String,
        info: PrintInfo,
        extraAnswers: String?
    ): String {
        val systemPrompt = """
            Eres un experto en impresión 3D FDM. La impresora del usuario es una Creality Ender-3 S1 Pro.
            Recibirás la foto de una impresión que falló y algunos datos que escribió el usuario.
            Identifica la falla más probable (por ejemplo: stringing, warping, mala adhesión de la primera capa,
            subextrusión, desplazamiento de capas, etc.). Si la foto no muestra una impresión 3D o no se ve
            ninguna falla, dilo claramente en el diagnóstico.
            Si te falta información clave para estar seguro, haz como máximo 2 preguntas breves en
            follow_up_questions; si no te falta nada, deja esa lista vacía.
            Responde SIEMPRE en español de México y SOLO con un objeto JSON con esta forma:
            {"diagnosis": "...", "confidence": "alta|media|baja", "explanation": "...", "advice": ["...", "..."], "follow_up_questions": []}
        """.trimIndent()

        val userText = buildString {
            appendLine("Datos que dio el usuario sobre esta impresión:")
            appendLine("- Filamento: ${info.filament}")
            if (info.nozzleTemp.isNotBlank()) appendLine("- Temperatura de boquilla: ${info.nozzleTemp} °C")
            if (info.bedTemp.isNotBlank()) appendLine("- Temperatura de cama: ${info.bedTemp} °C")
            if (info.notes.isNotBlank()) appendLine("- Notas: ${info.notes}")
            if (!extraAnswers.isNullOrBlank()) {
                appendLine()
                appendLine(extraAnswers)
            }
            appendLine()
            append("Analiza la foto y diagnostica la falla.")
        }

        val imagePart = JSONObject().put(
            "inline_data",
            JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", imageBase64)
        )
        val textPart = JSONObject().put("text", userText)
        val userContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(imagePart).put(textPart))

        return JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            .put("contents", JSONArray().put(userContent))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.3)
            )
            .toString()
    }

    // ---------- 2) Enviarla ----------

    private fun post(body: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val hint = if (code == 429) " (llegaste al límite gratis; espera un rato)" else ""
                throw IllegalStateException("Gemini respondió $code$hint: ${text.take(300)}")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    // ---------- 3) Leer la respuesta ----------

    private fun parseResult(responseText: String): DiagnosisResult {
        val root = JSONObject(responseText)
        val parts = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")

        val text = buildString {
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text", "") ?: "")
                }
            }
        }
        if (text.isBlank()) {
            throw IllegalStateException("Gemini no devolvió texto (puede ser un bloqueo de seguridad)")
        }

        // Por si el modelo envuelve el JSON en ```json ... ```
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)

        return DiagnosisResult(
            diagnosis = json.optString("diagnosis", "Sin diagnóstico"),
            confidence = json.optString("confidence", ""),
            explanation = json.optString("explanation", ""),
            advice = json.optJSONArray("advice").toStringList(),
            followUpQuestions = json.optJSONArray("follow_up_questions").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { i -> optString(i) }.filter { it.isNotBlank() }
    }

    // ---------- Preparar la foto: reducir, girar y convertir a Base64 ----------

    private fun jpegBase64(path: String, maxSide: Int = 1280): String {
        // Medimos la foto sin cargarla completa
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: throw IllegalStateException("No se pudo leer la foto")

        // Rotación según los datos EXIF
        val orientation = try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // Reducimos para que el envío sea rápido y gaste pocos tokens
        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postRotate(degrees)
            postScale(scale, scale)
        }
        val finalBitmap = Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, matrix, true
        )

        val output = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
