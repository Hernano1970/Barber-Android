package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object NotificationHelper {
    fun scheduleAll(context: Context) {
        val appSettings = AppSettings(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // We will handle exact alarm permission inside scheduleExactAlarm method


        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val appointments = db.appointmentDao().getAllAppointments().first()
            val _services = db.serviceDao().getActiveServices().first()
            val clients = db.clientDao().getAllClients().first()

            val now = System.currentTimeMillis()

            // 1. Turn Reminders
            if (appSettings.turnReminderEnabled) {
                // We should cancel old alarms conceptually, but for simplicity we'll just schedule upcoming
                appointments.filter { it.dateTimestamp > now && it.status == "Pendiente" }
                    .forEach { appt ->
                        val reminderTime = appt.dateTimestamp - (appSettings.turnReminderMinutes * 60 * 1000L)
                        if (reminderTime > now) {
                            val clientName = clients.find { it.id == appt.clientId }?.fullName ?: "Cliente"
                            val serviceName = _services.find { it.id == appt.serviceId }?.name ?: "Servicio"
                            val timeStr = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(appt.dateTimestamp))

                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                action = "TURN_REMINDER_${appt.id + 10000}"
                                putExtra("title", "Próximo turno en ${appSettings.turnReminderMinutes} minutos")
                                putExtra("message", "$clientName - $serviceName a las $timeStr hs.")
                                putExtra("notificationId", (appt.id + 10000))
                                putExtra("appointmentId", appt.id)
                            }
                            scheduleExactAlarm(context, alarmManager, intent, reminderTime, appt.id + 10000)
                        }
                    }
            }

            // 2. Daily Start Reminder
            if (appSettings.dailyStartReminderEnabled) {
                val parts = appSettings.dailyStartReminderTime.split(":")
                if (parts.size == 2) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                        set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        set(Calendar.SECOND, 0)
                    }
                    if (cal.timeInMillis < now) cal.add(Calendar.DAY_OF_YEAR, 1)

                    // Find tomorrow's count
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "DAILY_START_10001"
                        putExtra("action_type", "daily_start")
                        putExtra("title", "Inicio de Jornada")
                        putExtra("notificationId", 10001)
                    }
                    scheduleExactAlarm(context, alarmManager, intent, cal.timeInMillis, 10001)
                }
            }

            // 3. Daily Summary Reminder
            if (appSettings.dailySummaryEnabled) {
                val parts = appSettings.dailySummaryTime.split(":")
                if (parts.size == 2) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 20)
                        set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        set(Calendar.SECOND, 0)
                    }
                    if (cal.timeInMillis < now) cal.add(Calendar.DAY_OF_YEAR, 1)

                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "DAILY_SUMMARY_10002"
                        putExtra("action_type", "daily_summary")
                        putExtra("title", "Resumen del Día")
                        putExtra("notificationId", 10002)
                    }
                    scheduleExactAlarm(context, alarmManager, intent, cal.timeInMillis, 10002)
                }
            }

            // 4. Absences and Blocks
            appSettings.absencesList.filter { it.end + 86400000L > now }.forEach { absence ->
                // Extensive absence
                var targetTime = absence.start - (appSettings.absenceReminderDays * 86400000L)
                if (appSettings.absenceReminderTimeType == "InicioJornada") {
                    val cal = Calendar.getInstance().apply { timeInMillis = targetTime }
                    val parts = appSettings.dailyStartReminderTime.split(":")
                    if (parts.size == 2) {
                        cal.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                        cal.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        targetTime = cal.timeInMillis
                    }
                }

                val typeStr = if (absence.isPartial) "Bloqueo parcial programado" else "Vacaciones programadas"
                val df = java.text.SimpleDateFormat("dd/MM")
                
                if (targetTime > now) {
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "ABSENCE_REMINDER_${absence.id.hashCode()}"
                        putExtra("title", typeStr)
                        putExtra("message", "A partir del ${df.format(java.util.Date(absence.start))}.")
                        putExtra("notificationId", absence.id.hashCode())
                        putExtra("absenceId", absence.id)
                    }
                    scheduleExactAlarm(context, alarmManager, intent, targetTime, absence.id.hashCode())
                } else if (!appSettings.isAbsenceNotified(absence.id)) {
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "ABSENCE_REMINDER_${absence.id.hashCode()}"
                        putExtra("title", typeStr)
                        putExtra("message", "A partir del ${df.format(java.util.Date(absence.start))}.")
                        putExtra("notificationId", absence.id.hashCode())
                        putExtra("absenceId", absence.id)
                    }
                    context.sendBroadcast(intent)
                    appSettings.setAbsenceNotified(absence.id)
                }
                
                // For partials, remind them today (maybe 1 hour before)
                if (absence.isPartial) {
                    val parts = absence.startTime.split(":")
                    if (parts.size == 2) {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = absence.start
                            set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                            set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        }
                        val partialTime = cal.timeInMillis - (60 * 60 * 1000L) // 1 hour before
                        if (partialTime > now) {
                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                action = "PARTIAL_ABSENCE_${absence.id.hashCode() + 1}"
                                putExtra("title", "Bloqueo Próximo")
                                putExtra("message", "Tienes un bloqueo parcial de ${absence.startTime} a ${absence.endTime}.")
                                putExtra("notificationId", (absence.id.hashCode() + 1))
                                putExtra("absenceId", absence.id)
                            }
                            scheduleExactAlarm(context, alarmManager, intent, partialTime, absence.id.hashCode() + 1)
                        }
                    }
                }
            }
            
            // 5. Automatic Backup
            appSettings.autoBackupFrequency.let { freq ->
                if (freq != "Desactivado") {
                    val cal = Calendar.getInstance()
                    val parts = appSettings.autoBackupTime.split(":")
                    val hour = parts[0].toIntOrNull() ?: 2
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    if (cal.timeInMillis <= now) {
                        when (freq) {
                            "Diario" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                            "Semanal" -> cal.add(Calendar.DAY_OF_YEAR, 7)
                            "Mensual" -> cal.add(Calendar.MONTH, 1) // Rough approx
                        }
                    }

                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "AUTO_BACKUP_10003"
                        putExtra("action_type", "auto_backup")
                        putExtra("notificationId", 10003)
                    }
                    scheduleExactAlarm(context, alarmManager, intent, cal.timeInMillis, 10003)
                }
            }
        }
    }

    private fun scheduleExactAlarm(context: Context, alarmManager: AlarmManager, intent: Intent, time: Long, reqCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback to inexact if exact is denied
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }
}
