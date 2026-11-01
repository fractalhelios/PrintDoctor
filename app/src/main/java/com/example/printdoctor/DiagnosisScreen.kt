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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

// Los tres estados posibles de la pantalla
private sealed interface UiState {
    object Loading : UiState
    data class Success(val result: DiagnosisResult) : UiState
    data class Error(val message: String) : UiState
}

@Composable
fun DiagnosisScreen(
    photoPath: String,
    info: PrintInfo,
    onBack: () -> Unit,       // volver al formulario
    onNewPhoto: () -> Unit    // volver a la cámara
) {
    BackHandler(onBack = onBack)

    // Para cambiar de IA después, solo cambia esta línea (por ejemplo a ClaudeProvider())
    val provider: DiagnosisProvider = remember { GeminiProvider() }

    var state by remember { mutableStateOf<UiState>(UiState.Loading) }
    var attempt by remember { mutableStateOf(0) }            // cada cambio relanza el diagnóstico
    var extraAnswers by remember { mutableStateOf<String?>(null) }
    var answerText by remember { mutableStateOf("") }

    // Se ejecuta al abrir la pantalla y cada vez que "attempt" cambia
    LaunchedEffect(attempt) {
        state = UiState.Loading
        state = try {
            UiState.Success(provider.diagnose(photoPath, info, extraAnswers))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Error desconocido")
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

            // Fundido suave entre "analizando", "error" y "resultado"
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

                    is UiState.Error -> {
                        Column {
                            Text(
                                text = "No se pudo completar el diagnóstico:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(current.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppOutlinedButton(onClick = onBack) { Text("Volver") }
                                AppButton(onClick = { attempt++ }) { Text("Reintentar") }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val result = current.result

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

                            Spacer(modifier = Modifier.height(24.dp))
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
