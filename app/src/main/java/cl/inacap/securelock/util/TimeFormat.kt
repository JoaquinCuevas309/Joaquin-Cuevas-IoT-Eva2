package cl.inacap.securelock.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

fun relativeTime(then: Long, now: Long): String {
    val diff = (now - then).coerceAtLeast(0)
    return when {
        diff < MINUTE -> "justo ahora"
        diff < HOUR -> "hace ${diff / MINUTE} min"
        diff < DAY -> "hace ${diff / HOUR} h"
        diff < 7 * DAY -> "hace ${diff / DAY} d"
        else -> formatDate(then)
    }
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CL")).format(Date(millis))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL")).format(Date(millis))
