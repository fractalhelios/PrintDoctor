package com.example.printdoctor.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.printdoctor.R

// ---------------------------------------------------------------------------
// Los archivos .ttf van en app/src/main/res/font/ con estos nombres exactos
// (minúsculas, con guiones bajos):
//   playfair_display_semibold.ttf
//   raleway_light.ttf
//   raleway_semibold.ttf
// Si quieres probar otras tipografías, cambia aquí los nombres de R.font.xxx.
// ---------------------------------------------------------------------------

/** Para títulos elegantes (por ejemplo, el de la presentación). */
val TitleFont = FontFamily(
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold)
)

/** Para subtítulos, botones y etiquetas. */
val BodyFont = FontFamily(
    Font(R.font.raleway_light, FontWeight.Light),
    Font(R.font.raleway_semibold, FontWeight.SemiBold)
)

/**
 * Igual que la tipografía original, pero el estilo "labelLarge"
 * (el que usan los botones y los chips) ahora usa Raleway SemiBold
 * con un poco de espacio entre letras.
 */
val AppTypography = Typography.copy(
    labelLarge = Typography.labelLarge.copy(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.8.sp
    )
)
