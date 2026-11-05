package com.example.printdoctor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printdoctor.ui.theme.BodyFont

// Proporción de la foto (ancho / alto en píxeles de developer_photo.png).
// Si la cambias por otra con proporciones distintas, ajusta estos dos números.
private const val PortraitAspectRatio = 851f / 1053f

// Qué tan ancha es la foto respecto al espacio disponible (1f = todo el ancho).
// Antes era 0.85f; ahora 0.765f, que es 10 % más chica.
private const val PortraitWidthFraction = 0.765f

/**
 * Pantalla de créditos (a la que se llega con el huevo de pascua de Configuración).
 *
 * La foto es un recorte tipo sticker (PNG con fondo transparente y contorno):
 *  - res/drawable-nodpi/developer_photo.png        -> contorno blanco (tema oscuro)
 *  - res/drawable-nodpi/developer_photo_light.png  -> contorno negro (tema claro)
 * Están en "drawable-nodpi" para que Android no las agrande de más al cargarlas.
 */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    // Entrada: la foto crece con un pequeño rebote y, un momento después, aparece el texto
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val photoScale by animateFloatAsState(
        targetValue = if (show) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "photoScale"
    )
    val photoAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(500),
        label = "photoAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(700, delayMillis = 350),
        label = "textAlpha"
    )

    // ¿El tema que se está usando es claro? Se decide por la luminosidad del fondo
    // (así respeta también la opción de tema que elijas en Configuración, no solo la del sistema).
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Los dos espacios con "weight" empujan el contenido al centro de la pantalla
            Spacer(modifier = Modifier.weight(1f))

            // El contorno blanco se vería perdido sobre fondo claro, así que en tema claro
            // se usa la versión con contorno negro. aspectRatio le da la forma exacta
            // de la imagen para que nunca se deforme.
            Image(
                painter = painterResource(
                    if (isLightTheme) R.drawable.developer_photo_light
                    else R.drawable.developer_photo
                ),
                contentDescription = "Foto del desarrollador",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(PortraitWidthFraction)
                    .aspectRatio(PortraitAspectRatio)
                    .graphicsLayer {
                        scaleX = photoScale
                        scaleY = photoScale
                        alpha = photoAlpha
                    }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Dos líneas centradas, las dos en Raleway Bold:
            // "Desarrollador" más chico arriba y el nombre más grande abajo.
            // BodyFont incluye raleway_bold.ttf, así que FontWeight.Bold la elige sola.
            // (El espacio duro   evita que el nombre se parta en dos líneas.)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            ) {
                Text(
                    text = "Desarrollador",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Yarad Méndez",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AppOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
