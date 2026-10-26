package cl.inacap.securelock.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.ui.common.EmptyState
import cl.inacap.securelock.ui.common.EventoItem
import cl.inacap.securelock.ui.common.etiqueta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: (() -> Unit)? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial de accesos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (state.subtitulo.isNotBlank()) {
                            Text(state.subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TipoAcceso.entries.forEach { tipo ->
                    FilterChip(
                        selected = tipo in state.tipos,
                        onClick = { viewModel.toggleTipo(tipo) },
                        label = { Text(tipo.etiqueta()) },
                    )
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RangoFecha.entries.forEach { rango ->
                    FilterChip(
                        selected = state.rango == rango,
                        onClick = { viewModel.setRango(rango) },
                        label = { Text(rango.etiqueta) },
                    )
                }
            }
            if (state.eventos.isEmpty() && !state.cargando) {
                EmptyState("No hay accesos que coincidan con los filtros")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.eventos, key = { it.id }) { evento ->
                        EventoItem(evento, lockerNombre = if (state.esGlobal) state.nombresLocker[evento.lockerId] else null)
                    }
                }
            }
        }
    }
}
