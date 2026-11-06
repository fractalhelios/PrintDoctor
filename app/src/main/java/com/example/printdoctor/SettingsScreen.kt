package com.example.printdoctor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onOpenCredits: () -> Unit,   // el huevo de pascua de abajo lleva a los créditos
    onOpenKeyGuide: () -> Unit,  // pantalla guiada para conseguir la clave de Gemini
    onBack: () -> Unit
) {
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
                text = "Filamento",
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
            Spacer(modifier = Modifier.height(28.dp))

            // ---------------- Gemini AI ----------------
            SectionTitle("Gemini AI")
            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { settings.updateApiKey(it) },
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
            // "Probar clave" va en la misma fila que "Mostrar" y "Borrar clave".
            // Prueba la clave que realmente se va a usar (la guardada o la de local.properties).
            ApiKeyTester(
                apiKey = settings.effectiveApiKey(),
                extraButtons = {
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
            )
            // Solo se avisa cuando hay algo que decir (si se usa la de local.properties, nada)
            val keyHint = when {
                settings.apiKey.isNotBlank() -> "Clave guardada solo en este teléfono."
                settings.effectiveApiKey().isBlank() ->
                    "Todavía no hay clave: pégala arriba o sigue la guía de abajo."
                else -> null
            }
            if (keyHint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = keyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenKeyGuide) {
                Text("¿No tienes clave? Te explico cómo conseguirla")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "Volver" a la izquierda y, en la misma fila, el huevo de pascua a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppOutlinedButton(onClick = onBack) {
                    Text("Volver")
                }
                SecretDogButton(onClick = onOpenCredits)
            }
        }
    }
}

/**
 * Icono pequeño y discreto que abre los créditos.
 * Usa un recorte tipo sticker, así que se muestra con su propia silueta (sin círculo ni
 * borde dibujado):
 *  - res/drawable-nodpi/chihuahua_icon.png        -> contorno blanco (tema oscuro)
 *  - res/drawable-nodpi/chihuahua_icon_light.png  -> contorno negro (tema claro)
 */
@Composable
private fun SecretDogButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Con fondo claro el contorno blanco no se vería: se usa la versión con contorno negro
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Al presionarlo se encoge y regresa con un rebotito
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dogPress"
    )

    Image(
        painter = painterResource(
            if (isLightTheme) R.drawable.chihuahua_icon_light else R.drawable.chihuahua_icon
        ),
        contentDescription = "Créditos",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    )
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
