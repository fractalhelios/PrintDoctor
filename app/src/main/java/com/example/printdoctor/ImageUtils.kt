package com.example.printdoctor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

/**
 * Carga la foto reducida (para no gastar memoria de más) y la gira
 * si la cámara la guardó con otra orientación.
 * Llámala desde un hilo de fondo (Dispatchers.IO).
 */
fun loadScaledBitmap(path: String, maxSide: Int = 1600): Bitmap? {
    // 1) Medimos la foto sin cargarla
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)

    // 2) Calculamos cuánto reducirla
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) {
        sample *= 2
    }

    // 3) La cargamos ya reducida
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

    // 4) Corregimos la rotación según los datos EXIF
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
    if (degrees == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// ---------------------------------------------------------------------------------
//  Revisión rápida de la foto: ¿está muy oscura o borrosa?
// ---------------------------------------------------------------------------------

// Valores de partida (conservadores, para avisar solo cuando la foto está claramente mal).
// Si con tus fotos reales ves avisos de más o de menos, ajústalos aquí.
private const val DarkBrightnessThreshold = 35f  // brillo medio: 0 = negro total, 255 = blanco total
private const val BlurSharpnessThreshold = 40f   // nitidez (varianza del laplaciano a 512 px): menos = más borrosa

data class PhotoQuality(val brightness: Float, val sharpness: Float) {
    val tooDark: Boolean get() = brightness < DarkBrightnessThreshold
    val blurry: Boolean get() = sharpness < BlurSharpnessThreshold

    /** Los avisos que se le muestran al usuario (vacío = la foto se ve bien). */
    val problems: List<String>
        get() = buildList {
            if (tooDark) add("Se ve muy oscura: busca más luz o usa una lámpara.")
            if (blurry) add("Se ve borrosa: apoya el teléfono, acércate un poco y espera a que enfoque.")
        }
}

/**
 * Mide el brillo medio y la nitidez de la foto (reducida a 512 px, así el resultado no depende
 * de cuántos megapíxeles tenga la cámara). La nitidez es la varianza del "laplaciano": un filtro
 * que resalta los bordes; si casi no hay bordes marcados, la foto está borrosa.
 * Llámala desde un hilo de fondo (Dispatchers.IO). Devuelve null si no se pudo leer la foto.
 */
fun analyzePhotoQuality(path: String): PhotoQuality? {
    val loaded = loadScaledBitmap(path, 1024) ?: return null
    val scale = 512f / maxOf(loaded.width, loaded.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            loaded,
            (loaded.width * scale).toInt().coerceAtLeast(3),
            (loaded.height * scale).toInt().coerceAtLeast(3),
            true
        )
    } else {
        loaded
    }

    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    // Escala de grises y brillo medio
    val gray = FloatArray(w * h)
    var brightnessSum = 0.0
    for (i in pixels.indices) {
        val p = pixels[i]
        val g = 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
        gray[i] = g
        brightnessSum += g
    }
    val brightness = (brightnessSum / pixels.size).toFloat()

    // Laplaciano: 4 veces el pixel menos sus cuatro vecinos
    var lapSum = 0.0
    var lapSquareSum = 0.0
    var count = 0
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val i = y * w + x
            val lap = 4f * gray[i] - gray[i - 1] - gray[i + 1] - gray[i - w] - gray[i + w]
            lapSum += lap
            lapSquareSum += lap * lap
            count++
        }
    }
    val sharpness = if (count > 0) {
        val mean = lapSum / count
        ((lapSquareSum / count) - mean * mean).toFloat()
    } else {
        0f
    }

    return PhotoQuality(brightness, sharpness)
}
