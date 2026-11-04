package com.example.printdoctor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit) {
    // El botón "atrás" del teléfono también regresa a la pantalla principal
    BackHandler(onBack = onBack)

    var showKey by remember { mutableStateOf(false) }

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
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ---------------- Apariencia ----------------
            SectionTitle("Apariencia")
            Text(
                text = "Tema",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChipRow {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { settings.updateThemeMode(mode) },
                        label = { Text(mode.label) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // ---------------- Mi impresora ----------------
            SectionTitle("Mi impresora")
            Text(
                text = "Estos datos se envían a la IA para que el diagnóstico sea más preciso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.printerModel,
                onValueChange = { settings.updatePrinterModel(it) },
                label = { Text("Modelo de impresora") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Diámetro de la boquilla",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChipRow {
                NozzleOptions.forEach { option ->
                    FilterChip(
                        selected = settings.nozzleMm == option,
                        onClick = { settings.updateNozzle(option) },
                        label = { Text("$option mm") },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Filamento que usas casi siempre",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChipRow {
                FilamentOptions.forEach { option ->
                    val selected = settings.usualFilament == option
                    FilterChip(
                        selected = selected,
                        // Tocar de nuevo el seleccionado lo quita
                        onClick = { settings.updateUsualFilament(if (selected) null else option) },
                        label = { Text(option) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Aparecerá ya seleccionado en el formulario.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ---------------- Inteligencia artificial ----------------
            SectionTitle("Inteligencia artificial")
            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { settings.updateApiKey(it) },
                label = { Text("Clave de API de Gemini") },
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Ocultar" else "Mostrar")
                }
                TextButton(
                    onClick = { settings.updateApiKey("") },
                    enabled = settings.apiKey.isNotEmpty()
                ) {
                    Text("Borrar clave")
                }
            }
            Text(
                text = if (settings.apiKey.isBlank()) {
                    "Sin clave guardada en la app: se usará la de local.properties, si existe."
                } else {
                    "Clave guardada solo en este teléfono."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
            AppOutlinedButton(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}

// Título de cada sección
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
}

// Fila de chips que se puede desplazar si no caben todos
@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
