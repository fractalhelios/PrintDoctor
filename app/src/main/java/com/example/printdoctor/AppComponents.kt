package com.example.printdoctor

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

// ---- Colores del botón principal (puedes ajustarlos a tu gusto) ----
// Un azul más profundo que el fondo de la app, con degradado sutil y borde fino
// para que se distinga sin ser estridente.
private val ButtonBlueTop = Color(0xFF3A6AA0)
private val ButtonBlueBottom = Color(0xFF2A5183)
private val ButtonBorder = Color(0xFF6C9AD0).copy(alpha = 0.55f)
private val ButtonContent = Color(0xFFEAF2FB)

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

// Escala del botón: se encoge un poco al presionarlo y regresa con un muelle suave
@Composable
private fun rememberPressScale(interactionSource: MutableInteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
}

/**
 * Botón principal: degradado azul, borde fino, tipografía elegante
 * (la toma de AppTypography), animación al presionar y vibración leve.
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val haptic = LocalHapticFeedback.current
    val shape = MaterialTheme.shapes.medium

    // Activo: degradado azul. Desactivado: un tono neutro del tema.
    val fill: Brush = if (enabled) {
        Brush.verticalGradient(listOf(ButtonBlueTop, ButtonBlueBottom))
    } else {
        SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(brush = fill, shape = shape),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,   // el color lo pone el degradado de arriba
            contentColor = ButtonContent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (enabled) BorderStroke(1.dp, ButtonBorder) else null,
        elevation = null,
        contentPadding = ButtonPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Botón secundario: solo contorno, con los colores del tema
 * (se ve bien tanto en modo oscuro como en modo claro).
 */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = primary),
        border = BorderStroke(
            width = 1.5.dp,
            color = primary.copy(alpha = if (enabled) 0.6f else 0.2f)
        ),
        contentPadding = ButtonPadding,
        interactionSource = interactionSource,
        content = content
    )
}
