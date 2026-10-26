package cl.inacap.securelock.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cl.inacap.securelock.MainActivity
import cl.inacap.securelock.R

class AlertNotifier(private val context: Context) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.alert_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Android 13+ exige el permiso de notificaciones
    fun notifyAlarm(lockerId: String, lockerNombre: String) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_LOCKER_ID, lockerId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            lockerId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("⚠️ Alerta de seguridad")
            .setContentText("Apertura no autorizada en $lockerNombre")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(lockerId.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "alarmas"
    }
}
