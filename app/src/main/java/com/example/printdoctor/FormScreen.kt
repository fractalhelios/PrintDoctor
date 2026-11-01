package com.example.printdoctor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Los datos que captura el formulario. Se envían al modelo de IA.
data class PrintInfo(
    val filament: String,
    val nozzleTemp: String,
    val bedTemp: String,
    val notes: String
)

private val filamentOptions = listOf("PLA", "PETG", "ABS", "TPU", "Otro")

@Composable
fun FormScreen(
    photoPath: String,
    previewBitmap: Bitmap? = null,       // la foto ya cargada (evita parpadeos tras la animación)
    initialInfo: PrintInfo? = null,      // datos anteriores, para no perder lo que escribiste
    onBack: () -> Unit,
    onSubmit: (PrintInfo) -> Unit
) {
    // El botón "atrás" del teléfono vuelve a la cámara
    BackHandler(onBack = onBack)

    var filament by remember { mutableStateOf(initialInfo?.filament) }
    var nozzleTemp by remember { mutableStateOf(initialInfo?.nozzleTemp ?: "") }
    var bedTemp by remember { mutableStateOf(initialInfo?.bedTemp ?: "") }
    var notes by remember { mutableStateOf(initialInfo?.notes ?: "") }

    // Si ya traemos la foto cargada la usamos; si no, la cargamos en segundo plano
    val photoResult by produceState<Result<Bitmap?>?>(
        initialValue = previewBitmap?.let { Result.success(it) },
        photoPath
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { loadScaledBitmap(photoPath) }
            }
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
            // Fila superior: miniatura de la foto + título.
            // La miniatura es el destino de la animación de la foto, así que
            // debe ser el PRIMER elemento y tener el tamaño de PhotoThumbWidth/Height.
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(PhotoThumbWidth, PhotoThumbHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val result = photoResult
                    val bitmap = result?.getOrNull()
                    when {
                        result == null -> CircularProgressIndicator()
                        bitmap != null -> Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto de la impresión",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Text("Sin vista previa")
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Detalles de la impresión",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Elige el filamento y agrega lo que sepas. Mientras más datos, mejor el diagnóstico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Tipo de filamento (obligatorio)
            Text(
                text = "Tipo de filamento",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filamentOptions.forEach { option ->
                    FilterChip(
                        selected = filament == option,
                        onClick = { filament = option },
                        label = { Text(option) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Temperaturas (opcionales)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nozzleTemp,
                    onValueChange = { text -> nozzleTemp = text.filter { c -> c.isDigit() } },
                    label = { Text("Boquilla (°C)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bedTemp,
                    onValueChange = { text -> bedTemp = text.filter { c -> c.isDigit() } },
                    label = { Text("Cama (°C)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Notas (opcional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Otra foto")
                }
                AppButton(
                    onClick = {
                        filament?.let { selected ->
                            onSubmit(
                                PrintInfo(
                                    filament = selected,
                                    nozzleTemp = nozzleTemp,
                                    bedTemp = bedTemp,
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    enabled = filament != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Diagnosticar")
                }
            }
        }
    }
}
