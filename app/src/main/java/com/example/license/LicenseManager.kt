package com.example.license

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("license_prefs", Context.MODE_PRIVATE)

    enum class LicenseStatus {
        UNLICENSED,
        ACTIVE,
        EXPIRED
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        var id = prefs.getString("custom_device_id", null)
        if (id == null) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
            val padded = androidId.padEnd(8, '0')
            val formatted = "BAR-${padded.substring(0, 4).uppercase()}-${padded.substring(4, 8).uppercase()}"
            id = formatted
            prefs.edit().putString("custom_device_id", id).apply()
        }
        return id
    }

    fun getLicenseStatus(): LicenseStatus {
        val expirationDate = getExpirationDate() ?: return LicenseStatus.UNLICENSED
        
        val sdf = SimpleDateFormat(LicenseConfig.DATE_FORMAT, Locale.US)
        return try {
            val date = sdf.parse(expirationDate)
            if (date != null && date.before(Date())) {
                LicenseStatus.EXPIRED
            } else {
                LicenseStatus.ACTIVE
            }
        } catch (e: Exception) {
            LicenseStatus.UNLICENSED
        }
    }

    fun getExpirationDate(): String? {
        return prefs.getString("expiration_date", null)
    }

    fun getActivationDate(): String? {
        return prefs.getString("activation_date", null)
    }

    fun getRemainingDays(): Long {
        val expirationDate = getExpirationDate() ?: return 0
        val sdf = SimpleDateFormat(LicenseConfig.DATE_FORMAT, Locale.US)
        return try {
            val expDate = sdf.parse(expirationDate)
            if (expDate != null) {
                val diffInMillis = expDate.time - Date().time
                if (diffInMillis < 0) 0 else TimeUnit.MILLISECONDS.toDays(diffInMillis)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun activateLicense(licenseString: String): LicenseValidationResult {
        val currentDeviceId = getDeviceId()
        val result = LicenseSecurity.validateLicense(licenseString, currentDeviceId)

        if (result is LicenseValidationResult.Valid) {
            val sdf = SimpleDateFormat(LicenseConfig.DATE_FORMAT, Locale.US)
            
            try {
                val newDate = sdf.parse(result.expirationDate)
                if (newDate != null) {
                    val today = sdf.parse(sdf.format(Date()))
                    if (today != null && newDate.before(today)) {
                        return LicenseValidationResult.LicenseExpired
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors and continue
            }

            val currentExpirationStr = getExpirationDate()
            
            if (currentExpirationStr != null) {
                try {
                    val newDate = sdf.parse(result.expirationDate)
                    val currentDate = sdf.parse(currentExpirationStr)
                    
                    if (newDate != null && currentDate != null) {
                        if (newDate == currentDate) {
                            return LicenseValidationResult.AlreadyInstalled
                        } else if (newDate.before(currentDate)) {
                            return LicenseValidationResult.OlderLicense
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors and continue to activate
                }
            }

            val today = SimpleDateFormat(LicenseConfig.DATE_FORMAT, Locale.US).format(Date())
            prefs.edit()
                .putString("saved_license", licenseString)
                .putString("expiration_date", result.expirationDate)
                .putString("activation_date", today)
                .apply()
        }

        return result
    }

    fun shouldBlockApp(): Boolean {
        val status = getLicenseStatus()
        return status == LicenseStatus.EXPIRED || status == LicenseStatus.UNLICENSED
    }

    fun getWarningMessage(): String? {
        val status = getLicenseStatus()
        if (status == LicenseStatus.EXPIRED) {
            return "Licencia vencida. Contacte al desarrollador para renovar la activación."
        }
        
        if (status == LicenseStatus.ACTIVE) {
            val days = getRemainingDays()
            if (days == 10L || days == 5L || days == 3L || days == 1L || days == 0L) {
                val tiempoTexto = when (days) {
                    0L -> "hoy"
                    1L -> "mañana"
                    else -> "en $days días"
                }
                return "Su licencia vencerá $tiempoTexto. Contacte al desarrollador para renovarla."
            }
        }
        return null
    }
}
