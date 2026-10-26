package cl.inacap.securelock.ui.lockers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.ui.common.ConexionBadge
import cl.inacap.securelock.ui.common.EmptyState
import cl.inacap.securelock.ui.common.EstadoBadge
import cl.inacap.securelock.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockerListScreen(viewModel: LockerListViewModel, onLockerClick: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        val necesitaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (necesitaPermiso) permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis lockers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (state.nombreUsuario.isNotBlank()) {
                            Text(
                                state.nombreUsuario,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refrescando,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.cargando && state.lockers.isEmpty()) {
                    item {
                        EmptyState("Aún no tienes lockers asignados. Contacta al administrador", Modifier.fillParentMaxSize())
                    }
                }
                items(state.lockers, key = { it.id }) { locker -> LockerCard(locker) { onLockerClick(locker.id) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockerCard(locker: Locker, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(locker.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    locker.ultimoEvento?.let { "Último acceso: ${relativeTime(it, System.currentTimeMillis())}" } ?: "Sin accesos todavía",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EstadoBadge(locker.estado)
                ConexionBadge(locker.conectado)
            }
        }
    }
}
