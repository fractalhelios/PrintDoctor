package com.example.printdoctor.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.printdoctor.R

// ---------------------------------------------------------------------------
// Los archivos .ttf van en app/src/main/res/font/ con estos nombres exactos
// (minúsculas, con guiones bajos):
//   raleway_light.ttf
//   raleway_semibold.ttf
//   raleway_bold.ttf
// Si quieres probar otras tipografías, cambia aquí los nombres de R.font.xxx.
//
// Ya no se usa Playfair Display: puedes borrar playfair_display_semibold.ttf de
// res/font (si lo dejas no pasa nada, solo ocupa espacio en el APK).
// ---------------------------------------------------------------------------

/** Para títulos (por ejemplo, el de la presentación): ahora Raleway Bold. */
val TitleFont = FontFamily(
    Font(R.font.raleway_bold, FontWeight.Bold)
)

/** Para subtítulos, botones y etiquetas. */
val BodyFont = FontFamily(
    Font(R.font.raleway_light, FontWeight.Light),
    Font(R.font.raleway_semibold, FontWeight.SemiBold),
    Font(R.font.raleway_bold, FontWeight.Bold)
)

/**
 * Igual que la tipografía original, pero el estilo "labelLarge"
 * (el que usan los chips y, de base, los botones) ahora usa Raleway SemiBold
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
