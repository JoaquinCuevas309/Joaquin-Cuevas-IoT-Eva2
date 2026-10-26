package cl.inacap.securelock.ui.lockerdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.ui.common.ConexionBadge
import cl.inacap.securelock.ui.common.EstadoBadge
import cl.inacap.securelock.ui.common.EventoItem
import cl.inacap.securelock.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockerDetailScreen(
    viewModel: LockerDetailViewModel,
    onBack: () -> Unit,
    onVerHistorial: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locker = state.locker

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locker?.nombre ?: "Locker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (locker != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                EstadoBadge(locker.estado)
                                ConexionBadge(locker.conectado)
                            }
                        }
                        locker?.ultimoEvento?.let {
                            Text(
                                "Último acceso: ${relativeTime(it, System.currentTimeMillis())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.verificandoConexion) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Verificando conexión con el locker...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = viewModel::onAbrirClick,
                    enabled = state.botonHabilitado && locker != null,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                ) {
                    Text(
                        when {
                            locker?.conectado == false -> "Sin conexión"
                            locker?.comandoPendienteDesde != null -> "Enviando comando..."
                            locker?.estado == EstadoLocker.ABIERTO -> "Locker abierto"
                            else -> "Abrir"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (locker?.conectado == false) {
                    Text(
                        "El locker no está conectado. No se puede abrir hasta que vuelva a conectarse.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Últimos accesos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onVerHistorial) { Text("Ver historial completo") }
                }
            }
            if (state.ultimosAccesos.isEmpty() && !state.cargando) {
                item { Text("Todavía no hay accesos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.ultimosAccesos, key = { it.id }) { EventoItem(it) }

            if (state.esDemo) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Modo demo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Simula lo que haría el locker físico cuando se conecte el ESP32.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = viewModel::alternarConexion, modifier = Modifier.fillMaxWidth()) {
                                Text(if (locker?.conectado == false) "Simular reconexión" else "Simular desconexión")
                            }
                            OutlinedButton(onClick = viewModel::simularTarjeta, modifier = Modifier.fillMaxWidth()) { Text("Simular tarjeta RFID") }
                            OutlinedButton(onClick = viewModel::simularAlarma, modifier = Modifier.fillMaxWidth()) { Text("Simular apertura forzada") }
                        }
                    }
                }
            }
        }
    }
}
