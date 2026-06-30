package com.example.license

import org.junit.Test

class LicenseHashTest {
    @Test
    fun printHash() {
        val deviceId = "BAR-ABCD-1234"
        val expirationDate = "2026-08-07"
        val secretKey = "6B3A9C8D2F1E4A7B5C9D0E3F2A1B6C4D8E7F9A0B3C2D1E5F4A7B6C8D9E0F2A1B"
        val input = "$deviceId|$expirationDate|$secretKey"
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        val hexString = StringBuilder()
        for (byte in bytes) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        println("Cadena previa: " + input)
        println("Hash completo Hex: " + hexString.toString().uppercase())
        println("Firma final (8 chars): " + hexString.toString().substring(0, 8).uppercase())
    }
}
