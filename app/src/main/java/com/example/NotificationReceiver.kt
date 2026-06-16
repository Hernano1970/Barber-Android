package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.example.data.AppSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appSettings = AppSettings(context)
        
        // Handling boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            NotificationHelper.scheduleAll(context)
            return
        }

        val actionType = intent.getStringExtra("action_type")
        var title = intent.getStringExtra("title") ?: "BarberApp"
        var message = intent.getStringExtra("message") ?: ""
        val notificationId = intent.getIntExtra("notificationId", System.currentTimeMillis().toInt())

        val now = System.currentTimeMillis()
        val pendingResult = goAsync()
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (actionType == "auto_backup") {
                    com.example.BackupHelper.createBackup(context)
                    com.example.NotificationHelper.scheduleAll(context)
                    return@launch
                }

                if (actionType == "daily_start" || actionType == "daily_summary") {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val dao = db.appointmentDao()
                    
                    val todayStart = java.util.Calendar.getInstance().apply {
                        timeInMillis = now
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                    }.timeInMillis
                    val todayEnd = todayStart + 86400000L
                    
                    // In a real app we'd fetch only today's, for simplicity we collect
                    val appointments = dao.getAllAppointments().first()
                    val todayAppts = appointments.filter { it.dateTimestamp in todayStart..todayEnd }
                    
                    if (actionType == "daily_start") {
                        message = "Hoy tienes ${todayAppts.size} turnos programados."
                    } else if (actionType == "daily_summary") {
                        val completed = todayAppts.count { it.status == "Completado" }
                        val paid = todayAppts.count { it.isPaid }
                        message = "Turnos: ${todayAppts.size} | Completados: $completed | Pagos: $paid"
                    }
                }

                // Check if silent mode is active
                if (appSettings.silentModeUntil > now) {
                    return@launch
                }

                // Verify if absence still exists
                val absenceId = intent.getStringExtra("absenceId")
                if (absenceId != null) {
                    val exists = appSettings.absencesList.any { it.id == absenceId }
                    if (!exists) return@launch
                } else if (intent.action?.startsWith("ABSENCE_REMINDER_") == true) {
                    val reqCodeStr = intent.action?.removePrefix("ABSENCE_REMINDER_")
                    val reqCode = reqCodeStr?.toIntOrNull()
                    if (reqCode != null) {
                        val exists = appSettings.absencesList.any { it.id.hashCode() == reqCode }
                        if (!exists) return@launch
                    }
                } else if (intent.action?.startsWith("PARTIAL_ABSENCE_") == true) {
                    val reqCodeStr = intent.action?.removePrefix("PARTIAL_ABSENCE_")
                    val reqCode = reqCodeStr?.toIntOrNull()
                    if (reqCode != null) {
                        val exists = appSettings.absencesList.any { (it.id.hashCode() + 1) == reqCode }
                        if (!exists) return@launch
                    }
                }

                // Verify if appointment still exists and is correct status
                val appointmentId = intent.getIntExtra("appointmentId", -1)
                if (appointmentId != -1) {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val appt = db.appointmentDao().getAllAppointments().first().find { it.id == appointmentId }
                    if (appt == null || appt.status != "Pendiente") return@launch
                } else if (intent.action?.startsWith("TURN_REMINDER_") == true) {
                    val reqCodeStr = intent.action?.removePrefix("TURN_REMINDER_")
                    val reqCode = reqCodeStr?.toIntOrNull()
                    if (reqCode != null) {
                        val originalApptId = reqCode - 10000
                        val db = com.example.data.AppDatabase.getDatabase(context)
                        val appt = db.appointmentDao().getAllAppointments().first().find { it.id == originalApptId }
                        if (appt == null || appt.status != "Pendiente") return@launch
                    }
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "barberapp_notifications"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Notificaciones de Turnos y Recordatorios",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Centro de notificaciones de la app"
                        enableVibration(appSettings.vibrationEnabled)
                    }
                    if (!appSettings.soundEnabled) {
                        channel.setSound(null, null)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                if (!appSettings.soundEnabled) {
                    builder.setSound(null)
                }
                
                if (!appSettings.vibrationEnabled) {
                    builder.setVibrate(longArrayOf(0L))
                }

                notificationManager.notify(notificationId, builder.build())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
