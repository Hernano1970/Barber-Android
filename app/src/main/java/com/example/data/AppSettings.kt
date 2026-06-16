package com.example.data

import android.content.Context
import android.content.SharedPreferences

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.Calendar

data class Absence(
    val id: String = UUID.randomUUID().toString(),
    val start: Long,
    val end: Long,
    val type: String,
    val note: String,
    val isPartial: Boolean = false,
    val startTime: String = "",
    val endTime: String = ""
)

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("barberapp_settings", Context.MODE_PRIVATE)

    var absencesList: List<Absence>
        get() {
            val jsonString = prefs.getString("absencesList", "[]") ?: "[]"
            val list = mutableListOf<Absence>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(Absence(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        start = obj.getLong("start"),
                        end = obj.getLong("end"),
                        type = obj.getString("type"),
                        note = obj.getString("note"),
                        isPartial = obj.optBoolean("isPartial", false),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", "")
                    ))
                }
            } catch (e: Exception) { }
            return list
        }
        set(value) {
            val array = JSONArray()
            for (a in value) {
                val obj = JSONObject()
                obj.put("id", a.id)
                obj.put("start", a.start)
                obj.put("end", a.end)
                obj.put("type", a.type)
                obj.put("note", a.note)
                obj.put("isPartial", a.isPartial)
                obj.put("startTime", a.startTime)
                obj.put("endTime", a.endTime)
                array.put(obj)
            }
            prefs.edit().putString("absencesList", array.toString()).apply()
        }

    fun getAbsencesForDate(dateTimestamp: Long): List<Absence> {
        return absencesList.filter { absence -> 
            val startCal = Calendar.getInstance().apply {
                timeInMillis = absence.start
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = absence.end
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            dateTimestamp in startCal.timeInMillis..endCal.timeInMillis
        }
    }

    fun getFullDayAbsenceForDate(dateTimestamp: Long): Absence? {
        return getAbsencesForDate(dateTimestamp).firstOrNull { !it.isPartial }
    }

    var businessName: String
        get() = prefs.getString("businessName", "BarberApp Pro") ?: "BarberApp Pro"
        set(value) = prefs.edit().putString("businessName", value).apply()

    var turnReminderEnabled: Boolean
        get() = prefs.getBoolean("turnReminderEnabled", false)
        set(value) = prefs.edit().putBoolean("turnReminderEnabled", value).apply()

    var turnReminderMinutes: Int
        get() = prefs.getInt("turnReminderMinutes", 15)
        set(value) = prefs.edit().putInt("turnReminderMinutes", value).apply()
        
    var dailyStartReminderEnabled: Boolean
        get() = prefs.getBoolean("dailyStartReminderEnabled", false)
        set(value) = prefs.edit().putBoolean("dailyStartReminderEnabled", value).apply()
        
    var dailyStartReminderTime: String
        get() = prefs.getString("dailyStartReminderTime", "08:00") ?: "08:00"
        set(value) = prefs.edit().putString("dailyStartReminderTime", value).apply()
        
    var absenceReminderDays: Int
        get() = prefs.getInt("absenceReminderDays", 1)
        set(value) = prefs.edit().putInt("absenceReminderDays", value).apply()

    var absenceReminderTimeType: String
        get() = prefs.getString("absenceReminderTimeType", "00:00") ?: "00:00"
        set(value) = prefs.edit().putString("absenceReminderTimeType", value).apply()
        
    var notifiedAbsences: Set<String>
        get() = prefs.getStringSet("notifiedAbsences", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("notifiedAbsences", value).apply()
        
    fun setAbsenceNotified(id: String) {
        val current = notifiedAbsences.toMutableSet()
        current.add(id)
        notifiedAbsences = current
    }

    fun isAbsenceNotified(id: String): Boolean {
        return notifiedAbsences.contains(id)
    }

    var dailySummaryEnabled: Boolean
        get() = prefs.getBoolean("dailySummaryEnabled", false)
        set(value) = prefs.edit().putBoolean("dailySummaryEnabled", value).apply()
        
    var dailySummaryTime: String
        get() = prefs.getString("dailySummaryTime", "20:00") ?: "20:00"
        set(value) = prefs.edit().putString("dailySummaryTime", value).apply()
        
    var soundEnabled: Boolean
        get() = prefs.getBoolean("soundEnabled", true)
        set(value) = prefs.edit().putBoolean("soundEnabled", value).apply()
        
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibrationEnabled", true)
        set(value) = prefs.edit().putBoolean("vibrationEnabled", value).apply()
        
    var silentModeUntil: Long
        get() = prefs.getLong("silentModeUntil", 0)
        set(value) = prefs.edit().putLong("silentModeUntil", value).apply()

    var businessAddress: String
        get() = prefs.getString("businessAddress", "") ?: ""
        set(value) = prefs.edit().putString("businessAddress", value).apply()

    var businessPhone: String
        get() = prefs.getString("businessPhone", "") ?: ""
        set(value) = prefs.edit().putString("businessPhone", value).apply()

    var whatsappMessageTemplate: String
        get() = prefs.getString(
            "whatsappMessageTemplate",
            "Hola {nombre}, te recordamos tu turno para el {fecha} a las {hora}. Te esperamos en {negocio}."
        ) ?: ""
        set(value) = prefs.edit().putString("whatsappMessageTemplate", value).apply()

    var whatsappTransferTemplate: String
        get() = prefs.getString(
            "whatsappTransferTemplate",
            "¡Hola {nombre}!\n\nTe comparto mis datos para que puedas realizar el pago correspondiente a:\n\nServicio: {servicio}\n\nImporte: \${importe}\n\nBilletera: {billetera}\n\nAlias: {alias}\nCVU: {cvu}\nTitular: {titular}\n\nMuchas gracias."
        ) ?: ""
        set(value) = prefs.edit().putString("whatsappTransferTemplate", value).apply()

    var whatsappSentRecords: Map<Int, Long>
        get() {
            val jsonString = prefs.getString("whatsappSentRecords", "{}") ?: "{}"
            val map = mutableMapOf<Int, Long>()
            try {
                val obj = JSONObject(jsonString)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key.toInt()] = obj.getLong(key)
                }
            } catch (e: Exception) {}
            return map
        }
        set(value) {
            val obj = JSONObject()
            try {
                for ((k, v) in value) {
                    obj.put(k.toString(), v)
                }
            } catch (e: Exception) {}
            prefs.edit().putString("whatsappSentRecords", obj.toString()).apply()
        }

    fun markWhatsAppSent(appointmentId: Int) {
        val map = whatsappSentRecords.toMutableMap()
        map[appointmentId] = System.currentTimeMillis()
        whatsappSentRecords = map
    }

    fun getWhatsAppSentTime(appointmentId: Int): Long? {
        return whatsappSentRecords[appointmentId]
    }

    var statisticsStartDate: Long
        get() = prefs.getLong("statisticsStartDate", 0L)
        set(value) = prefs.edit().putLong("statisticsStartDate", value).apply()

    var backupLocation: String
        get() = prefs.getString("backupLocation", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS).absolutePath + "/BarberApp/Backups") ?: ""
        set(value) = prefs.edit().putString("backupLocation", value).apply()

    var autoBackupFrequency: String
        get() = prefs.getString("autoBackupFrequency", "Desactivado") ?: "Desactivado"
        set(value) = prefs.edit().putString("autoBackupFrequency", value).apply()

    var maxAutoBackups: Int
        get() = prefs.getInt("maxAutoBackups", 10)
        set(value) = prefs.edit().putInt("maxAutoBackups", value).apply()

    var autoBackupTime: String
        get() = prefs.getString("autoBackupTime", "02:00") ?: "02:00"
        set(value) = prefs.edit().putString("autoBackupTime", value).apply()

    // working hours (JSON string or simple delimited)
    // format: monday:08:00-20:00,tuesday...
    var workingHoursMap: String
        get() = prefs.getString(
            "workingHoursMap",
            "1|true|09:00|19:00,2|true|09:00|19:00,3|true|09:00|19:00,4|true|09:00|19:00,5|true|09:00|19:00,6|true|09:00|19:00,0|false|09:00|19:00"
        ) ?: ""
        set(value) = prefs.edit().putString("workingHoursMap", value).apply()

    fun getWorkingHours(dayOfWeek: Int): Pair<Int, Int>? {
        // dayOfWeek is java.util.Calendar.DAY_OF_WEEK. Sunday=1, Monday=2.. Saturday=7
        // In our map: Sunday=0, Monday=1.. Saturday=6
        val mapIndex = dayOfWeek - 1 
        
        val mapStr = workingHoursMap
        if (!mapStr.contains("|")) return Pair(8, 20) // fallback if old format

        val entries = mapStr.split(",").map { it.trim() }
        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.size >= 4) {
                val day = parts[0].toIntOrNull()
                if (day == mapIndex) {
                    val isEnabled = parts[1].toBooleanStrictOrNull() ?: true
                    if (isEnabled) {
                         val startHour = parts[2].split(":")[0].toIntOrNull() ?: 9
                         val endHour = parts[3].split(":")[0].toIntOrNull() ?: 19
                         return Pair(startHour, endHour)
                    } else {
                         return null
                    }
                }
            }
        }
        return null // Closed
    }
}
