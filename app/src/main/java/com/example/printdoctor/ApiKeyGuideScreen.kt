package com.example.printdoctor

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Página de Google AI Studio donde se crean las claves de la API de Gemini
private const val AiStudioKeysUrl = "https://aistudio.google.com/apikey"

// =====================================================================================
//  Pantalla guiada: cómo conseguir (y probar) tu clave de Gemini
// =====================================================================================

@Composable
fun ApiKeyGuideScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Lo que el usuario pega todavía NO se guarda: primero se prueba y luego decide guardarla
    var draft by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Columna de afuera: el contenido se desplaza y el botón queda fijo hasta abajo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Conseguir mi clave de Gemini",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "La clave es como una contraseña que le permite a PrintDoctor usar la IA de " +
                        "Google. Es fácil conseguirla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (settings.apiKey.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ya tienes una clave guardada. Si guardas otra, la reemplaza.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // ---- Paso 1 ----
                StepRow(number = 1, title = "Entra a Google AI Studio") {
                    Text("Inicia sesión con tu cuenta de Google.")
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedButton(
                        onClick = {
                            runCatching { uriHandler.openUri(AiStudioKeysUrl) }.onFailure {
                                Toast.makeText(
                                    context,
                                    "No encontré un navegador para abrir la página",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text("Abrir Google AI Studio")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // ---- Paso 2 ----
                StepRow(number = 2, title = "Crea tu clave") {
                    Text("Dale en Create API key.")
                }
                Spacer(modifier = Modifier.height(24.dp))

                // ---- Paso 3 ----
                StepRow(number = 3, title = "Pégala aquí y pruébala") {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            saved = false
                        },
                        singleLine = true,
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // "Probar clave" va en la misma fila que "Pegar" y "Mostrar"
                    ApiKeyTester(
                        apiKey = draft,
                        extraButtons = {
                            TextButton(onClick = {
                                val pasted = readClipboardText(context)
                                if (pasted == null) {
                                    Toast.makeText(context, "No hay nada para pegar", Toast.LENGTH_SHORT).show()
                                } else {
                                    draft = pasted
                                    saved = false
                                }
                            }) {
                                Text("Pegar")
                            }
                            TextButton(onClick = { showKey = !showKey }) {
                                Text(if (showKey) "Ocultar" else "Mostrar")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    AppButton(
                        onClick = {
                            settings.updateApiKey(draft.trim())
                            saved = true
                        },
                        enabled = draft.isNotBlank()
                    ) {
                        Text("Guardar clave")
                    }
                    if (saved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Clave guardada solo en este teléfono. Ya puedes diagnosticar.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Nunca compartas tu clave!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón fijo abajo, pequeño y del mismo tamaño que el "Volver" de Configuración
            Spacer(modifier = Modifier.height(12.dp))
            if (saved) {
                AppButton(onClick = onBack) { Text("Listo") }
            } else {
                AppOutlinedButton(onClick = onBack) { Text("Volver") }
            }
        }
    }
}

// Un paso numerado: círculo con el número a la izquierda y el contenido a la derecha
@Composable
private fun StepRow(
    number: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

// Lee el texto que el usuario copió (null si el portapapeles está vacío)
private fun readClipboardText(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.ifBlank { null }
}

// =====================================================================================
//  Botón "Probar clave" (lo usan esta pantalla y Configuración)
// =====================================================================================

private sealed interface KeyTestStatus {
    object Idle : KeyTestStatus
    object Testing : KeyTestStatus
    data class Done(val result: KeyTestResult) : KeyTestStatus
}

/**
 * Botón "Probar clave" (estilo texto, como "Mostrar" y "Borrar clave") que prueba la clave
 * recibida y muestra el resultado debajo. Si la clave cambia, el resultado anterior se borra.
 *
 * extraButtons: otros botones que se quieren en la MISMA fila, a la izquierda de "Probar clave".
 */
@Composable
fun ApiKeyTester(
    apiKey: String,
    modifier: Modifier = Modifier,
    extraButtons: @Composable RowScope.() -> Unit = {}
) {
    var status by remember(apiKey) { mutableStateOf<KeyTestStatus>(KeyTestStatus.Idle) }
    val scope = rememberCoroutineScope()
    val testing = status is KeyTestStatus.Testing

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            extraButtons()
            TextButton(
                onClick = {
                    status = KeyTestStatus.Testing
                    scope.launch { status = KeyTestStatus.Done(testGeminiKey(apiKey)) }
                },
                enabled = apiKey.isNotBlank() && !testing
            ) {
                Text(if (testing) "Probando…" else "Probar clave")
            }
        }

        when (val current = status) {
            KeyTestStatus.Idle -> Unit

            KeyTestStatus.Testing -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probando con Google…", style = MaterialTheme.typography.bodySmall)
                }
            }

            is KeyTestStatus.Done -> {
                Spacer(modifier = Modifier.height(8.dp))
                when (val result = current.result) {
                    is KeyTestResult.Valid -> Text(
                        text = "✓ La clave funciona." + (result.note?.let { " $it" } ?: ""),
                        color = MaterialTheme.colorScheme.primary
                    )

                    is KeyTestResult.Invalid -> Text(
                        text = "✗ ${result.message}",
                        color = MaterialTheme.colorScheme.error
                    )

                    is KeyTestResult.Failed -> Text(
                        text = "No se pudo probar: ${result.message}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
