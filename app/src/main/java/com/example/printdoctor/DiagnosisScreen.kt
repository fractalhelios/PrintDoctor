package com.example.printdoctor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Los cuatro estados posibles de la pantalla
private sealed interface UiState {
    object Loading : UiState

    /** La foto se ve muy oscura o borrosa: se avisa ANTES de gastar una consulta a la IA. */
    data class PhotoWarning(val problems: List<String>) : UiState

    data class Success(val result: DiagnosisResult) : UiState

    /** Un error ya traducido a español (ver DiagnosisError.kt). */
    data class Error(val error: DiagnosisException) : UiState
}

@Composable
fun DiagnosisScreen(
    photoPath: String,
    info: PrintInfo,
    provider: DiagnosisProvider,          // la IA que hace el diagnóstico (Gemini hoy, Claude mañana)
    onDiagnosed: (DiagnosisResult) -> Unit = {},   // se llama cada vez que llega un resultado
    onBack: () -> Unit,                   // volver al formulario
    onNewPhoto: () -> Unit,               // volver a la cámara
    photoWarningAccepted: Boolean = false,         // ¿ya se dijo "usar de todos modos" con esta foto?
    onPhotoWarningAccepted: () -> Unit = {},
    onOpenKeyGuide: () -> Unit = {}       // abre la guía para conseguir/arreglar la clave
) {
    BackHandler(onBack = onBack)

    var state by remember { mutableStateOf<UiState>(UiState.Loading) }
    var attempt by remember { mutableStateOf(0) }            // cada cambio relanza el diagnóstico
    var extraAnswers by remember { mutableStateOf<String?>(null) }
    var answerText by remember { mutableStateOf("") }
    // La revisión de la foto se hace una sola vez (o se salta si ya se aceptó el aviso)
    var photoChecked by remember { mutableStateOf(photoWarningAccepted) }

    // El efecto de abajo puede tardar varios segundos en terminar.
    // rememberUpdatedState hace que, cuando termine, llame a la versión MÁS RECIENTE
    // de onDiagnosed y no a una copia vieja que quedó "congelada" al empezar.
    val currentOnDiagnosed by rememberUpdatedState(onDiagnosed)

    // Se ejecuta al abrir la pantalla y cada vez que "attempt" cambia
    LaunchedEffect(attempt) {
        state = UiState.Loading

        // 1) Revisión rápida de la foto (en segundo plano, toma una fracción de segundo)
        if (!photoChecked) {
            val quality = withContext(Dispatchers.IO) {
                runCatching { analyzePhotoQuality(photoPath) }.getOrNull()
            }
            if (quality != null && quality.problems.isNotEmpty()) {
                state = UiState.PhotoWarning(quality.problems)
                return@LaunchedEffect       // espera a que el usuario decida
            }
            photoChecked = true
        }

        // 2) El diagnóstico en sí
        state = try {
            val result = provider.diagnose(photoPath, info, extraAnswers)
            currentOnDiagnosed(result)      // aquí MainActivity lo guarda en el historial
            UiState.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(diagnosisErrorFromException(e))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = "Diagnóstico", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Fundido suave entre "analizando", "aviso", "error" y "resultado"
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(150))
                },
                label = "diagnosisState"
            ) { current ->
                when (current) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analizando la foto…")
                        }
                    }

                    is UiState.PhotoWarning -> {
                        Column {
                            Text(
                                text = "Esta foto podría no servir bien",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            current.problems.forEach { problem ->
                                Text("• $problem")
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Con una foto así el diagnóstico puede salir menos preciso. " +
                                    "Si puedes, toma otra con buena luz y bien enfocada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            // Botones uno sobre otro para que el texto nunca se parta en dos líneas
                            AppButton(onClick = onNewPhoto, modifier = Modifier.fillMaxWidth()) {
                                Text("Tomar otra")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AppOutlinedButton(
                                onClick = {
                                    photoChecked = true
                                    onPhotoWarningAccepted()
                                    attempt++
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar de todos modos")
                            }
                        }
                    }

                    is UiState.Error -> {
                        val error = current.error
                        Column {
                            Text(
                                text = "No se pudo completar el diagnóstico",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error.userMessage, color = MaterialTheme.colorScheme.error)
                            // El detalle técnico va chiquito, por si hay que depurar
                            error.technicalDetail?.let { detail ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            if (error.needsKeyFix) {
                                // Reintentar no serviría de nada con una clave mala o ausente
                                AppButton(
                                    onClick = onOpenKeyGuide,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Configurar mi clave")
                                }
                            } else {
                                AppButton(
                                    onClick = { attempt++ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reintentar")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AppOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                                Text("Volver")
                            }
                        }
                    }

                    is UiState.Success -> {
                        val result = current.result

                        Column {
                            DiagnosisResultBody(result)

                            // Si el modelo necesita más datos, hace preguntas
                            if (result.followUpQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    "Para afinar el diagnóstico necesito saber:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                result.followUpQuestions.forEach { question ->
                                    Text("• $question")
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = answerText,
                                    onValueChange = { answerText = it },
                                    label = { Text("Tu respuesta") },
                                    minLines = 2,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AppButton(
                                    onClick = {
                                        val questions =
                                            result.followUpQuestions.joinToString("\n") { "- $it" }
                                        extraAnswers = "Preguntas que hiciste antes:\n$questions\n" +
                                            "Respuesta del usuario: ${answerText.trim()}"
                                        answerText = ""
                                        attempt++
                                    },
                                    enabled = answerText.isNotBlank()
                                ) {
                                    Text("Responder y diagnosticar de nuevo")
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Este diagnóstico quedó guardado en tu historial. " +
                                    "Ahí puedes marcar si acertó.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppOutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Editar datos")
                                }
                                AppButton(
                                    onClick = onNewPhoto,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Nueva foto")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * La parte del resultado que no cambia: diagnóstico, confianza, explicación y consejos.
 * La usan esta pantalla y la de detalle del historial, para que se vean idénticas.
 */
@Composable
fun DiagnosisResultBody(result: DiagnosisResult) {
    Column {
        Text(result.diagnosis, style = MaterialTheme.typography.titleLarge)
        if (result.confidence.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Confianza: ${result.confidence}")
        }
        if (result.explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(result.explanation)
        }

        if (result.advice.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Qué hacer la próxima vez",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.advice.forEach { tip ->
                Text("• $tip")
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
