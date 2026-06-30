package com.example.license

import java.security.MessageDigest

object LicenseSecurity {

    /**
     * Genera la firma para un dispositivo y fecha de vencimiento dados.
     * Algoritmo: SHA-256(deviceId + "|" + expirationDate + "|" + SECRET_KEY)
     * Resultado: Hexadecimal en mayúsculas, tomando los primeros 8 caracteres.
     */
    fun generateSignature(deviceId: String, expirationDate: String): String {
        val input = "$deviceId|$expirationDate|${LicenseConfig.SECRET_KEY}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        val hexString = StringBuilder()
        for (byte in bytes) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString().substring(0, 8).uppercase()
    }

    /**
     * Valida si una licencia tiene el formato correcto y su firma coincide.
     */
    fun validateLicense(licenseString: String, currentDeviceId: String): LicenseValidationResult {
        val parts = licenseString.split("|")
        if (parts.size != 3) {
            return LicenseValidationResult.InvalidFormat
        }

        val deviceId = parts[0]
        val expirationDate = parts[1]
        val signature = parts[2]

        if (deviceId != currentDeviceId) {
            return LicenseValidationResult.DeviceMismatch
        }

        // Validar formato de fecha
        try {
            val sdf = java.text.SimpleDateFormat(LicenseConfig.DATE_FORMAT, java.util.Locale.US)
            sdf.isLenient = false
            sdf.parse(expirationDate)
        } catch (e: Exception) {
            return LicenseValidationResult.InvalidDate
        }

        val expectedSignature = generateSignature(deviceId, expirationDate)
        if (signature != expectedSignature) {
            return LicenseValidationResult.InvalidSignature
        }

        return LicenseValidationResult.Valid(expirationDate)
    }
}

sealed class LicenseValidationResult {
    data class Valid(val expirationDate: String) : LicenseValidationResult()
    object InvalidFormat : LicenseValidationResult()
    object DeviceMismatch : LicenseValidationResult()
    object InvalidDate : LicenseValidationResult()
    object InvalidSignature : LicenseValidationResult()
    object AlreadyInstalled : LicenseValidationResult()
    object OlderLicense : LicenseValidationResult()
    object LicenseExpired : LicenseValidationResult()
}
