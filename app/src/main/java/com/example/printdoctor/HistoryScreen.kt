package com.example.printdoctor

import android.graphics.Bitmap
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =====================================================================================
//  Pantalla 1: la cuadrícula de miniaturas
// =====================================================================================

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onOpen: (HistoryEntry) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    // Exportar a CSV: Android abre su selector de archivos para elegir dónde guardarlo
    // (no hace falta ningún permiso extra).
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            // El carácter \uFEFF (BOM) hace que Excel lea bien los acentos
                            out.write(("\uFEFF" + buildHistoryCsv(entries)).toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) "Historial exportado" else "No se pudo guardar el archivo",
                    Toast.LENGTH_SHORT
                ).show()
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
                .padding(16.dp)
        ) {
            Text(text = "Historial", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (entries.size) {
                    0 -> "Aquí se guardan tus diagnósticos."
                    1 -> "1 diagnóstico guardado. Tócalo para verlo."
                    else -> "${entries.size} diagnósticos guardados. Toca uno para verlo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                // Aún no hay nada guardado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Todavía no hay diagnósticos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toma una foto de una impresión y aparecerá aquí.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // LazyVerticalGrid solo dibuja las miniaturas que se ven en pantalla,
                // así que funciona igual de bien con 10 diagnósticos que con 500.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.photoPath }) { entry ->
                        HistoryCard(entry = entry, onClick = { onOpen(entry) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (entries.isNotEmpty()) {
                AppOutlinedButton(
                    onClick = { exportLauncher.launch("historial_printdoctor.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar historial (CSV)")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            AppOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}

// Colores de la insignia "¿acertó?" (se ven bien en tema claro y oscuro)
private fun verdictColor(verdict: Verdict): Color = when (verdict) {
    Verdict.Yes -> Color(0xFF3FA66B)
    Verdict.Partial -> Color(0xFFD9A441)
    Verdict.No -> Color(0xFFD9605B)
}

// Una miniatura con su fecha y el título del diagnóstico
@Composable
private fun HistoryCard(entry: HistoryEntry, onClick: () -> Unit) {
    // Misma animación de "botón que se hunde" que usan los demás botones
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardPress"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        PhotoThumbnail(
            path = entry.photoPath,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        )
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = formatDate(entry.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.result.diagnosis,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,      // todas las tarjetas miden lo mismo
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Siempre hay una línea aquí (aunque diga "Sin calificar") para que todas midan igual
            Text(
                text = entry.verdict?.badge ?: "Sin calificar",
                style = MaterialTheme.typography.bodySmall,
                color = entry.verdict?.let { verdictColor(it) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================================
//  Pantalla 2: un diagnóstico guardado
// =====================================================================================

@Composable
fun HistoryDetailScreen(
    entry: HistoryEntry,
    onBack: () -> Unit,
    onFeedback: (Verdict?, String) -> Unit,    // guarda "¿acertó?" y la causa real
    onDelete: () -> Unit
) {
    BackHandler(onBack = onBack)

    var confirmDelete by remember { mutableStateOf(false) }

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
            Text(text = "Diagnóstico guardado", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDate(entry.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // La foto completa (sin recortar)
            PhotoThumbnail(
                path = entry.photoPath,
                maxSide = 1600,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(16.dp))

            PrintInfoSummary(entry.info)

            DiagnosisResultBody(entry.result)

            Spacer(modifier = Modifier.height(24.dp))
            FeedbackSection(entry = entry, onSave = onFeedback)

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Eliminar")
                }
                AppButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Volver")
                }
            }
        }
    }

    // Confirmación antes de borrar (no se puede deshacer)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este diagnóstico?") },
            text = { Text("También se borrará la foto de este teléfono. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

// "¿Acertó el diagnóstico?": tres opciones y un campo para la causa real
@Composable
private fun FeedbackSection(entry: HistoryEntry, onSave: (Verdict?, String) -> Unit) {
    // Lo que se está editando (empieza igual a lo guardado)
    var verdict by remember(entry.photoPath) { mutableStateOf(entry.verdict) }
    var cause by remember(entry.photoPath) { mutableStateOf(entry.actualCause) }

    val trimmed = cause.trim()
    val dirty = verdict != entry.verdict || trimmed != entry.actualCause
    val hasSaved = entry.verdict != null || entry.actualCause.isNotBlank()

    Text("¿Acertó el diagnóstico?", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Cuando sepas qué pasó en realidad, anótalo aquí. Sirve para llevar la cuenta de " +
            "qué tan bien acierta la app, y se incluye al exportar el historial.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Verdict.entries.forEach { option ->
            FilterChip(
                selected = verdict == option,
                // Tocar de nuevo el seleccionado lo quita
                onClick = { verdict = if (verdict == option) null else option },
                label = { Text(option.label) },
                shape = MaterialTheme.shapes.small
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = cause,
        onValueChange = { cause = it },
        label = { Text("Causa real (opcional)") },
        placeholder = { Text("Ej.: la cama estaba mal nivelada") },
        minLines = 2,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppButton(onClick = { onSave(verdict, trimmed) }, enabled = dirty) {
            Text("Guardar")
        }
        if (!dirty && hasSaved) {
            Text("✓ Guardado", color = MaterialTheme.colorScheme.primary)
        }
    }
}

// Resumen de lo que se escribió en el formulario (solo muestra lo que no está vacío)
@Composable
private fun PrintInfoSummary(info: PrintInfo) {
    val items = buildList {
        if (info.filament.isNotBlank()) add("Filamento" to info.filament)
        if (info.nozzleTemp.isNotBlank()) add("Temp. de boquilla" to "${info.nozzleTemp} °C")
        if (info.bedTemp.isNotBlank()) add("Temp. de cama" to "${info.bedTemp} °C")
        if (info.notes.isNotBlank()) add("Notas" to info.notes)
    }
    if (items.isEmpty()) return

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Text("Datos de la impresión", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    items.forEach { (label, value) ->
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = labelColor)) {
                    append("$label: ")
                }
                append(value)
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// =====================================================================================
//  Miniaturas: se cargan en segundo plano y se guardan en memoria
// =====================================================================================

private sealed interface ThumbState {
    object Loading : ThumbState
    object Missing : ThumbState           // la foto ya no existe en el teléfono
    class Ready(val bitmap: Bitmap) : ThumbState
}

/**
 * Memoria de miniaturas ya cargadas. Si vuelves a la cuadrícula, aparecen al instante
 * en lugar de volver a leerse del almacenamiento. Usa como máximo 1/8 de la memoria de la app;
 * cuando se llena, desecha primero las que hace más tiempo no se ven (LRU).
 */
private object ThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private fun keyOf(path: String, maxSide: Int) = "$path@$maxSide"

    fun get(path: String, maxSide: Int): Bitmap? = cache.get(keyOf(path, maxSide))

    // Llamar desde un hilo de fondo
    fun load(path: String, maxSide: Int): Bitmap? {
        cache.get(keyOf(path, maxSide))?.let { return it }
        val bitmap = runCatching { loadScaledBitmap(path, maxSide) }.getOrNull() ?: return null
        cache.put(keyOf(path, maxSide), bitmap)
        return bitmap
    }
}

private fun loadThumbState(path: String, maxSide: Int): ThumbState {
    val bitmap = ThumbnailCache.load(path, maxSide)
    return if (bitmap != null) ThumbState.Ready(bitmap) else ThumbState.Missing
}

@Composable
fun PhotoThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 800,
    contentScale: ContentScale = ContentScale.Crop
) {
    // produceState lanza una corrutina que va dejando el resultado en un "estado":
    // empieza en Loading (o ya en Ready, si la foto estaba en la memoria) y cambia cuando termina.
    val state by produceState<ThumbState>(
        initialValue = ThumbnailCache.get(path, maxSide)?.let { ThumbState.Ready(it) }
            ?: ThumbState.Loading,
        path,
        maxSide
    ) {
        if (value is ThumbState.Loading) {
            value = withContext(Dispatchers.IO) { loadThumbState(path, maxSide) }
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = state, animationSpec = tween(250), label = "thumb") { current ->
            when (current) {
                is ThumbState.Ready -> {
                    val image = remember(current.bitmap) { current.bitmap.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = "Foto de la impresión",
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ThumbState.Missing -> Text(
                    text = "Foto no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
                is ThumbState.Loading -> Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// "4 nov 2026 · 17:54", en español de México
private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX")).format(Date(millis))
