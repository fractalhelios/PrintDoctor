package com.example.printdoctor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printdoctor.ui.theme.BodyFont
import com.example.printdoctor.ui.theme.MistBlue
import com.example.printdoctor.ui.theme.NightBlue
import com.example.printdoctor.ui.theme.NightBlueLight
import com.example.printdoctor.ui.theme.SoftBlue
import com.example.printdoctor.ui.theme.TitleFont
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Logo provisional. Cuando tengas tu PNG con transparencia, cópialo a
 * app/src/main/res/drawable con el nombre logo.png (minúsculas, sin espacios)
 * y reemplaza el contenido de esta función por:
 *
 * Image(
 *     painter = painterResource(R.drawable.logo),
 *     contentDescription = "Logo de PrintDoctor",
 *     modifier = modifier.size(size)
 * )
 *
 * (agrega los imports androidx.compose.foundation.Image y
 *  androidx.compose.ui.res.painterResource)
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 250.dp) {

    Image(
        painter = painterResource(R.drawable.logo_dog_full),
        contentDescription = "Logo de PrintDoctor",
        modifier = modifier.size(size)
    )
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Opacidad y escala del logo (entra con fundido y un zoom suave)
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    // Al final, el subtítulo y la barra se desvanecen (1 -> 0)
    val fadeOut = remember { Animatable(1f) }
    // Desplazamiento vertical (en píxeles) de logo y título
    val logoOffset = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(0f) }

    // Distancia suficiente para que el logo y el título salgan de la pantalla
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val travelPx = screenHeightPx * 0.75f

    LaunchedEffect(Unit) {
        // Fase 1: todo aparece mientras la barra carga
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing)) }
            launch {
                delay(350)
                titleAlpha.animateTo(1f, tween(durationMillis = 800))
            }
            launch {
                delay(700)
                subtitleAlpha.animateTo(1f, tween(durationMillis = 800))
            }
            launch { progress.animateTo(1f, tween(durationMillis = 2500, easing = LinearEasing)) }
        }

        // Fase 2: la barra y el subtítulo se van; el logo sube primero
        // y el título lo sigue con un pequeño retraso
        coroutineScope {
            launch { fadeOut.animateTo(0f, tween(durationMillis = 300)) }
            launch {
                logoOffset.animateTo(
                    -travelPx,
                    tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(150) // <- el retraso del título
                titleOffset.animateTo(
                    -travelPx,
                    tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
        }

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightBlue, NightBlueLight))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo con un halo de luz suave detrás
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        translationY = logoOffset.value
                    }
            ) {
                AppLogo()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "PrintDoctor",
                color = Color(0xFFE1E8F0),
                fontSize = 42.sp,
                fontFamily = TitleFont,              // ahora Raleway Bold (ver AppFonts.kt)
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffset.value
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Diagnostico de errores en impresiones 3D.",
                color = MistBlue,
                fontSize = 15.5.sp,
                fontFamily = BodyFont,               // BodyFont incluye raleway_bold
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value * fadeOut.value
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .width(220.dp)
                    .graphicsLayer { alpha = fadeOut.value },
                color = SoftBlue,
                trackColor = Color(0xFF263648)
            )
        }
    }
}
