package cl.inacap.securelock.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.ui.theme.LocalStatusColors
import cl.inacap.securelock.util.formatDateTime

@Composable
fun EstadoBadge(estado: EstadoLocker, modifier: Modifier = Modifier) {
    val colors = LocalStatusColors.current
    val (color, label) = when (estado) {
        EstadoLocker.CERRADO -> colors.closed to "Cerrado"
        EstadoLocker.ABIERTO -> colors.open to "Abierto"
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ConexionBadge(conectado: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalStatusColors.current
    val color = if (conectado) colors.online else colors.offline
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (conectado) color else MaterialTheme.colorScheme.surface)
                .border(1.5.dp, color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (conectado) "Conectado" else "Sin conexión",
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

fun TipoAcceso.etiqueta() = when (this) {
    TipoAcceso.RFID -> "Tarjeta RFID"
    TipoAcceso.APP -> "App"
    TipoAcceso.ALARMA -> "Alarma"
}

fun TipoAcceso.icono(): ImageVector = when (this) {
    TipoAcceso.RFID -> Icons.Filled.AccountBox
    TipoAcceso.APP -> Icons.Filled.Phone
    TipoAcceso.ALARMA -> Icons.Filled.Warning
}

@Composable
fun EventoItem(evento: AccesoEvento, modifier: Modifier = Modifier, lockerNombre: String? = null) {
    val esAlarma = evento.tipo == TipoAcceso.ALARMA
    val fondo = if (esAlarma) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val contenido = if (esAlarma) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(evento.tipo.icono(), contentDescription = null, tint = contenido)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (esAlarma) "Alarma · apertura no autorizada" else "${evento.tipo.etiqueta()} · ${evento.usuario ?: "sin usuario"}",
                color = contenido,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (esAlarma) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = listOfNotNull(lockerNombre, formatDateTime(evento.timestamp)).joinToString(" · "),
                color = contenido.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
