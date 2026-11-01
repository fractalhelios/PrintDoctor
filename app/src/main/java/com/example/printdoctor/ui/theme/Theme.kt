package com.example.printdoctor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colores de marca (la pantalla de presentación también los usa)
val NightBlue = Color(0xFF0F1823)
val NightBlueLight = Color(0xFF1B2C42)
val SoftBlue = Color(0xFF8FB4E3)
val MistBlue = Color(0xFFB4C2D3)

private val DarkColors = darkColorScheme(
    primary = SoftBlue,
    onPrimary = Color(0xFF0B2545),
    primaryContainer = Color(0xFF2B4A73),
    onPrimaryContainer = Color(0xFFD6E6FA),
    secondary = Color(0xFF9DB7D6),
    onSecondary = Color(0xFF14283F),
    secondaryContainer = Color(0xFF2A3F58),
    onSecondaryContainer = Color(0xFFD8E5F5),
    tertiary = Color(0xFF86C5D8),
    onTertiary = Color(0xFF05303B),
    background = NightBlue,
    onBackground = Color(0xFFE1E8F0),
    surface = Color(0xFF131E2B),
    onSurface = Color(0xFFE1E8F0),
    surfaceVariant = Color(0xFF263648),
    onSurfaceVariant = MistBlue,
    surfaceContainer = Color(0xFF182535),
    surfaceContainerHigh = Color(0xFF1F2F42),
    outline = Color(0xFF7A8CA1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D6A9E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FA),
    onPrimaryContainer = Color(0xFF0B2A4A),
    secondary = Color(0xFF55657A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F2),
    onSecondaryContainer = Color(0xFF121D2C),
    tertiary = Color(0xFF2F7185),
    onTertiary = Color.White,
    background = Color(0xFFF5F8FC),
    onBackground = Color(0xFF151C25),
    surface = Color(0xFFF5F8FC),
    onSurface = Color(0xFF151C25),
    surfaceVariant = Color(0xFFDEE5EF),
    onSurfaceVariant = Color(0xFF424C59),
    surfaceContainer = Color(0xFFE9EFF7),
    surfaceContainerHigh = Color(0xFFE2E9F3),
    outline = Color(0xFF727D8B)
)

// Esquinas redondeadas, pero sin exagerar
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PrintDoctorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Ya no usamos el color dinámico del fondo de pantalla:
    // la app mantiene siempre su propia paleta azul.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
