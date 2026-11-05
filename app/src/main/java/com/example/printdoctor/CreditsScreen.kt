package com.example.printdoctor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printdoctor.ui.theme.SoftBlue
import com.example.printdoctor.ui.theme.TitleFont

/**
 * Pantalla de créditos (a la que se llega con el huevo de pascua de Configuración).
 *
 * La foto es res/drawable/developer_photo.xml (un dibujo de relleno por ahora).
 * Para poner la tuya: borra ese .xml y agrega developer_photo.png con el MISMO nombre.
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

            Image(
                painter = painterResource(R.drawable.developer_photo),
                contentDescription = "Foto del desarrollador",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = photoScale
                        scaleY = photoScale
                        alpha = photoAlpha
                    }
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(SoftBlue, Color(0xFF3A6AA0))),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Los espacios "duros" ( ) evitan que el nombre se parta en dos líneas feas
            // en pantallas angostas: si no cabe todo en una línea, corta después del guion.
            Text(
                text = "Desarrollador - Yarad Méndez",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = TitleFont,
                    fontSize = 21.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            )

            Spacer(modifier = Modifier.weight(1f))

            AppOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
