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
    val note: String
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
                        note = obj.getString("note")
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
                array.put(obj)
            }
            prefs.edit().putString("absencesList", array.toString()).apply()
        }

    fun getAbsenceForDate(dateTimestamp: Long): Absence? {
        return absencesList.find { absence -> 
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

    fun isAbsenceDate(dateTimestamp: Long): Boolean {
        return getAbsenceForDate(dateTimestamp) != null
    }

    var businessName: String
        get() = prefs.getString("businessName", "BarberApp Pro") ?: "BarberApp Pro"
        set(value) = prefs.edit().putString("businessName", value).apply()

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
