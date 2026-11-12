package com.example.warehouse.utils

import java.security.MessageDigest

object PasswordUtils {
    private const val SALT = "WarehouseSecretSalt_2026_Secure!"

    fun hashPassword(password: String): String {
        val salted = password + SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(inputPassword: String, storedPasswordOrHash: String): Boolean {
        if (storedPasswordOrHash.isBlank()) return false
        val hashedInput = hashPassword(inputPassword)
        if (hashedInput.equals(storedPasswordOrHash, ignoreCase = true)) return true
        // Fallback for legacy plain text stored passwords
        return inputPassword == storedPasswordOrHash
    }
}
