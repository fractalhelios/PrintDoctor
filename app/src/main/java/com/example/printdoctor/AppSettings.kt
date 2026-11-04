package com.example.printdoctor

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Modos de tema que se pueden elegir en Configuración. */
enum class ThemeMode(val label: String) {
    System("Sistema"),
    Light("Claro"),
    Dark("Oscuro")
}

// Opciones compartidas por el formulario y la configuración
val FilamentOptions = listOf("PLA", "PETG", "ABS", "TPU", "Otro")
val NozzleOptions = listOf("0.2", "0.4", "0.6", "0.8")

/** Datos de la impresora que se envían al modelo para mejorar el diagnóstico. */
data class PrinterProfile(
    val model: String,
    val nozzleMm: String,
    val usualFilament: String?
)

/**
 * Guarda las opciones de Configuración en el almacenamiento privado de la app
 * (SharedPreferences). Cada propiedad es un "estado" de Compose: si cambia,
 * las pantallas que la usan se actualizan solas.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("printdoctor_settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(readTheme())
        private set

    var apiKey by mutableStateOf(prefs.getString(KEY_API_KEY, "") ?: "")
        private set

    var printerModel by mutableStateOf(
        prefs.getString(KEY_PRINTER_MODEL, "Creality Ender-3 S1 Pro") ?: ""
    )
        private set

    var nozzleMm by mutableStateOf(prefs.getString(KEY_NOZZLE, "0.4") ?: "0.4")
        private set

    var usualFilament by mutableStateOf(prefs.getString(KEY_FILAMENT, null))
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun updateApiKey(value: String) {
        apiKey = value.trim()
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun updatePrinterModel(value: String) {
        printerModel = value
        prefs.edit().putString(KEY_PRINTER_MODEL, value).apply()
    }

    fun updateNozzle(value: String) {
        nozzleMm = value
        prefs.edit().putString(KEY_NOZZLE, value).apply()
    }

    // null = sin filamento habitual
    fun updateUsualFilament(value: String?) {
        usualFilament = value
        prefs.edit().putString(KEY_FILAMENT, value).apply()
    }

    /** La clave que se usa: la de Configuración o, si está vacía, la de local.properties. */
    fun effectiveApiKey(): String = apiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

    fun printerProfile() = PrinterProfile(printerModel, nozzleMm, usualFilament)

    private fun readTheme(): ThemeMode {
        val saved = prefs.getString(KEY_THEME, ThemeMode.System.name) ?: ThemeMode.System.name
        return runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.System)
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_API_KEY = "api_key"
        const val KEY_PRINTER_MODEL = "printer_model"
        const val KEY_NOZZLE = "nozzle_mm"
        const val KEY_FILAMENT = "usual_filament"
    }
}
