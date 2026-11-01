package com.example.printdoctor

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.example.printdoctor.ui.theme.PrintDoctorTheme

// Las pantallas de la app. El ORDEN importa: define hacia dónde se deslizan las transiciones.
enum class Screen { Home, Form, Diagnosis, Settings }

// Etapas de la presentación: mostrándose -> abriéndose el "iris" -> terminada
private enum class SplashPhase { Showing, Revealing, Done }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrintDoctorTheme {
                var screen by remember { mutableStateOf(Screen.Home) }
                var splashPhase by remember { mutableStateOf(SplashPhase.Showing) }
                val iris = remember { Animatable(0f) }

                // Ruta y versión ya cargada de la última foto (las comparten Home, Form y Diagnosis)
                var photoPath by remember { mutableStateOf<String?>(null) }
                var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
                // Últimos datos del formulario
                var printInfo by remember { mutableStateOf<PrintInfo?>(null) }
                // Animación de la foto volando hacia el formulario (null = no hay animación)
                var flight by remember { mutableStateOf<PhotoFlight?>(null) }

                // Cuando la presentación termina, el iris se abre desde el centro
                LaunchedEffect(splashPhase) {
                    if (splashPhase == SplashPhase.Revealing) {
                        iris.animateTo(
                            1f,
                            tween(durationMillis = 800, easing = FastOutSlowInEasing)
                        )
                        splashPhase = SplashPhase.Done
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    // Capa de abajo: la presentación (se queda hasta que el iris termina de abrirse)
                    if (splashPhase != SplashPhase.Done) {
                        SplashScreen(onFinished = { splashPhase = SplashPhase.Revealing })
                    }

                    // Capa de arriba: la app. Se crea hasta que la presentación termina,
                    // y entra con el efecto de iris (círculo que crece con un ligero zoom).
                    if (splashPhase != SplashPhase.Showing) {
                        val revealModifier = if (splashPhase == SplashPhase.Revealing) {
                            Modifier
                                .clip(CircleRevealShape(iris.value))
                                .graphicsLayer {
                                    val s = lerp(1.12f, 1f, iris.value)
                                    scaleX = s
                                    scaleY = s
                                }
                        } else {
                            Modifier
                        }

                        Box(modifier = Modifier.fillMaxSize().then(revealModifier)) {
                            AnimatedContent(
                                targetState = screen,
                                transitionSpec = {
                                    val toForm = initialState == Screen.Home &&
                                        targetState == Screen.Form
                                    val forward = targetState.ordinal > initialState.ordinal
                                    when {
                                        // Home -> Form: solo fundido, porque la foto aterriza
                                        // en un punto fijo de la pantalla
                                        toForm -> fadeIn(tween(250)) togetherWith fadeOut(tween(250))

                                        // Avanzar: la pantalla nueva entra desde la derecha
                                        forward -> (
                                            slideInHorizontally(
                                                tween(350, easing = FastOutSlowInEasing)
                                            ) { it / 6 } + fadeIn(tween(300))
                                            ) togetherWith (
                                            slideOutHorizontally(
                                                tween(350, easing = FastOutSlowInEasing)
                                            ) { -it / 6 } + fadeOut(tween(200))
                                            )

                                        // Regresar: entra desde la izquierda
                                        else -> (
                                            slideInHorizontally(
                                                tween(350, easing = FastOutSlowInEasing)
                                            ) { -it / 6 } + fadeIn(tween(300))
                                            ) togetherWith (
                                            slideOutHorizontally(
                                                tween(350, easing = FastOutSlowInEasing)
                                            ) { it / 6 } + fadeOut(tween(200))
                                            )
                                    }
                                },
                                label = "screens"
                            ) { current ->
                                when (current) {
                                    Screen.Home -> HomeScreen(
                                        onOpenSettings = { screen = Screen.Settings },
                                        onPhotoCaptured = { path, bitmap, from, to ->
                                            photoPath = path
                                            photoBitmap = bitmap
                                            if (bitmap != null) {
                                                // La foto "congelada" vuela hacia el formulario
                                                flight = PhotoFlight(bitmap, from, to)
                                            } else {
                                                screen = Screen.Form
                                            }
                                        }
                                    )

                                    Screen.Form -> {
                                        val path = photoPath
                                        if (path != null) {
                                            FormScreen(
                                                photoPath = path,
                                                previewBitmap = photoBitmap,
                                                initialInfo = printInfo,
                                                onBack = { screen = Screen.Home },
                                                onSubmit = { info ->
                                                    printInfo = info
                                                    screen = Screen.Diagnosis
                                                }
                                            )
                                        }
                                    }

                                    Screen.Diagnosis -> {
                                        val path = photoPath
                                        val info = printInfo
                                        if (path != null && info != null) {
                                            DiagnosisScreen(
                                                photoPath = path,
                                                info = info,
                                                onBack = { screen = Screen.Form },
                                                onNewPhoto = { screen = Screen.Home }
                                            )
                                        }
                                    }

                                    Screen.Settings -> SettingsScreen(
                                        onBack = { screen = Screen.Home }
                                    )
                                }
                            }
                        }
                    }

                    // Capa más alta: la foto volando (queda por encima de todo)
                    flight?.let { current ->
                        PhotoFlightOverlay(
                            flight = current,
                            onArrived = { screen = Screen.Form },
                            onFinished = { flight = null }
                        )
                    }
                }
            }
        }
    }
}
