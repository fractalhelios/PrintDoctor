package com.example.printdoctor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** El resultado de probar una clave de Gemini. */
sealed interface KeyTestResult {
    /** La clave funciona (note = un aviso extra, por ejemplo que ya se acabó el cupo gratis). */
    data class Valid(val note: String? = null) : KeyTestResult

    /** Google rechazó la clave (o no se escribió ninguna). */
    data class Invalid(val message: String) : KeyTestResult

    /** No se pudo probar (sin internet, fallo de Google...): no se sabe si la clave sirve. */
    data class Failed(val message: String) : KeyTestResult
}

/**
 * Prueba una clave pidiéndole a Gemini la lista de modelos con un solo resultado.
 * Esa consulta no cuesta nada: no gasta tokens ni cuenta como un diagnóstico.
 *
 * Se llama desde Configuración y desde la pantalla guiada ("Probar clave").
 */
suspend fun testGeminiKey(apiKey: String): KeyTestResult = withContext<KeyTestResult>(Dispatchers.IO) {
    val key = apiKey.trim()
    if (key.isEmpty()) {
        return@withContext KeyTestResult.Invalid("Primero escribe o pega tu clave.")
    }

    try {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("x-goog-api-key", key)

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code in 200..299) {
                KeyTestResult.Valid()
            } else {
                val problem = diagnosisErrorFromHttp(code, body)
                when (problem.kind) {
                    DiagnosisErrorKind.InvalidKey -> KeyTestResult.Invalid(problem.userMessage)
                    // 429 significa que Google SÍ reconoció la clave, pero ya no hay cupo
                    DiagnosisErrorKind.RateLimit -> KeyTestResult.Valid(
                        "Ojo: ya llegaste al límite de solicitudes gratis, pero la clave es válida."
                    )
                    else -> KeyTestResult.Failed(problem.userMessage)
                }
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        KeyTestResult.Failed(diagnosisErrorFromException(e).userMessage)
    }
}
