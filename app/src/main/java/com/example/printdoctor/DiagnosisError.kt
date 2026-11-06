package com.example.printdoctor

import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Los tipos de problema que pueden pasar al pedir un diagnóstico.
 * Cada uno tiene su propio mensaje en español para que el usuario sepa qué hacer,
 * en lugar de ver un texto técnico en inglés.
 */
enum class DiagnosisErrorKind {
    NoKey,          // todavía no hay una clave guardada
    InvalidKey,     // Google rechazó la clave
    RateLimit,      // se llegó al límite de solicitudes (el nivel gratuito tiene tope)
    ModelNotFound,  // el nombre del modelo ya no existe (error 404)
    NoInternet,     // sin conexión
    Timeout,        // Gemini tardó demasiado
    Server,         // falla del lado de Google (errores 5xx)
    Blocked,        // la IA se negó a analizar la foto (filtro de seguridad)
    BadResponse,    // llegó algo que la app no pudo entender
    Unknown
}

/**
 * Un error de diagnóstico "ya traducido":
 *  - userMessage: lo que se le muestra al usuario.
 *  - technicalDetail: el detalle técnico (código HTTP, texto de Google...), útil para depurar.
 */
class DiagnosisException(
    val kind: DiagnosisErrorKind,
    val userMessage: String,
    val technicalDetail: String? = null,
    cause: Throwable? = null
) : Exception(userMessage, cause) {

    /** Si es verdad, reintentar no sirve de nada hasta arreglar la clave. */
    val needsKeyFix: Boolean
        get() = kind == DiagnosisErrorKind.NoKey || kind == DiagnosisErrorKind.InvalidKey
}

/** Traduce una respuesta HTTP de error de Gemini a un DiagnosisException. */
fun diagnosisErrorFromHttp(code: Int, body: String): DiagnosisException {
    val detail = "Respuesta $code: ${body.trim().take(300)}"
    val keyRejected = body.contains("API key not valid", ignoreCase = true) ||
        body.contains("API_KEY_INVALID") ||
        body.contains("API key expired", ignoreCase = true)

    return when {
        keyRejected || code == 401 -> DiagnosisException(
            DiagnosisErrorKind.InvalidKey,
            "Google rechazó la clave de Gemini. Revisa que esté completa y bien copiada, o genera una nueva.",
            detail
        )

        code == 403 -> DiagnosisException(
            DiagnosisErrorKind.InvalidKey,
            "La clave no tiene permiso para usar Gemini (error 403). Genera una nueva en Google AI Studio.",
            detail
        )

        code == 404 -> DiagnosisException(
            DiagnosisErrorKind.ModelNotFound,
            "No se encontró el modelo de IA configurado (error 404). Los nombres de modelo cambian " +
                "seguido: actualiza el nombre en GeminiProvider.",
            detail
        )

        code == 429 -> DiagnosisException(
            DiagnosisErrorKind.RateLimit,
            "Llegaste al límite de solicitudes gratis. Espera un par de minutos y reintenta " +
                "(si ya usaste todas las del día, vuelve a intentar mañana).",
            detail
        )

        code in 500..599 -> DiagnosisException(
            DiagnosisErrorKind.Server,
            "Los servidores de Gemini tienen problemas en este momento (error $code). Reintenta en un momento.",
            detail
        )

        code == 400 && body.contains("location is not supported", ignoreCase = true) -> DiagnosisException(
            DiagnosisErrorKind.Unknown,
            "Gemini no está disponible desde tu ubicación con este tipo de acceso.",
            detail
        )

        else -> DiagnosisException(
            DiagnosisErrorKind.Unknown,
            "Gemini respondió con un error inesperado ($code).",
            detail
        )
    }
}

/** Traduce cualquier excepción (sin internet, tiempo agotado, JSON raro...) a un DiagnosisException. */
fun diagnosisErrorFromException(e: Throwable): DiagnosisException = when (e) {
    is DiagnosisException -> e

    is UnknownHostException, is ConnectException -> DiagnosisException(
        DiagnosisErrorKind.NoInternet,
        "No hay conexión a internet. Conéctate a Wi-Fi o a tus datos móviles y reintenta.",
        e.javaClass.simpleName,
        e
    )

    is SocketTimeoutException -> DiagnosisException(
        DiagnosisErrorKind.Timeout,
        "Gemini tardó demasiado en responder. Reintenta; si pasa seguido, revisa tu conexión.",
        e.javaClass.simpleName,
        e
    )

    is SSLException -> DiagnosisException(
        DiagnosisErrorKind.NoInternet,
        "No se pudo abrir una conexión segura con Google. Si estás en un Wi-Fi público, " +
            "quizá te pida iniciar sesión primero.",
        e.message,
        e
    )

    is IOException -> DiagnosisException(
        DiagnosisErrorKind.NoInternet,
        "Se cortó la conexión con Gemini. Reintenta.",
        e.message,
        e
    )

    is JSONException -> DiagnosisException(
        DiagnosisErrorKind.BadResponse,
        "La IA devolvió una respuesta que la app no pudo entender. Reintenta; suele arreglarse sola.",
        e.message,
        e
    )

    else -> DiagnosisException(
        DiagnosisErrorKind.Unknown,
        e.message ?: "Error desconocido",
        e.javaClass.simpleName,
        e
    )
}
