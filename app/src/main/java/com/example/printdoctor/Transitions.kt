package com.example.printdoctor

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

// Tamaño de la miniatura de la foto en el formulario (proporción 3:4, igual que la foto)
val PhotoThumbWidth = 132.dp
val PhotoThumbHeight = 176.dp

/**
 * Efecto "iris": un círculo que crece desde el centro y va revelando la pantalla.
 * fraction = 0 (cerrado) ... 1 (abierto por completo)
 */
class CircleRevealShape(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val maxRadius = hypot(size.width, size.height) / 2f
        val radius = maxRadius * fraction
        val center = Offset(size.width / 2f, size.height / 2f)
        return Outline.Generic(Path().apply { addOval(Rect(center = center, radius = radius)) })
    }
}

/** Datos del vuelo de la foto: imagen, rectángulo de origen y rectángulo de destino (en píxeles). */
data class PhotoFlight(val bitmap: Bitmap, val from: Rect, val to: Rect)

/**
 * La foto "congelada" se encoge y vuela desde el visor de la cámara hasta
 * la miniatura del formulario.
 *
 * - onArrived: la foto ya llegó; es el momento de cambiar a la pantalla del formulario.
 * - onFinished: el vuelo terminó por completo; ya se puede quitar esta capa.
 */
@Composable
fun PhotoFlightOverlay(
    flight: PhotoFlight,
    onArrived: () -> Unit,
    onFinished: () -> Unit
) {
    val progress = remember(flight) { Animatable(0f) }
    val overlayAlpha = remember(flight) { Animatable(1f) }
    val density = LocalDensity.current
    val shape = MaterialTheme.shapes.medium
    val imageBitmap = remember(flight) { flight.bitmap.asImageBitmap() }

    LaunchedEffect(flight) {
        progress.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
        onArrived()
        delay(400) // damos tiempo a que el formulario termine de aparecer debajo
        overlayAlpha.animateTo(0f, tween(durationMillis = 150))
        onFinished()
    }

    val widthPx = lerp(flight.from.width, flight.to.width, progress.value)
    val heightPx = lerp(flight.from.height, flight.to.height, progress.value)

    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .offset {
                IntOffset(
                    lerp(flight.from.left, flight.to.left, progress.value).roundToInt(),
                    lerp(flight.from.top, flight.to.top, progress.value).roundToInt()
                )
            }
            .size(
                width = with(density) { widthPx.toDp() },
                height = with(density) { heightPx.toDp() }
            )
            .alpha(overlayAlpha.value)
            .clip(shape)
    )
}
