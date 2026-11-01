package com.example.printdoctor

// Lo que devuelve el modelo de IA ya ordenado
data class DiagnosisResult(
    val diagnosis: String,
    val confidence: String,              // "alta", "media" o "baja"
    val explanation: String,
    val advice: List<String>,
    val followUpQuestions: List<String>
)

/**
 * Cualquier servicio de IA que quieras usar (Gemini hoy, Claude mañana)
 * solo tiene que implementar esta interfaz. El resto de la app no cambia.
 */
interface DiagnosisProvider {
    suspend fun diagnose(
        photoPath: String,
        info: PrintInfo,
        extraAnswers: String? = null
    ): DiagnosisResult
}
