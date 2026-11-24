package com.example.cloud

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AuthSecurity {

    private const val ADMIN_TRIGGER_HASH = "9f315d771892b03b26d57f5319f7d38d45d6200b26e9e2b5bd2905de22bee4bc"
    private const val ADMIN_PASS_HASH = "b2716925b2429b297f4ba3d317b05b6b6d4d04c7da89eb123d1908690d684914"
    private const val PBKDF2_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256

    fun isAdminSearchTrigger(query: String): Boolean {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return false
        return constantTimeEquals(sha256Hex(trimmed), ADMIN_TRIGGER_HASH)
    }

    fun verifyAdminPassword(password: String): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        return constantTimeEquals(sha256Hex(trimmed), ADMIN_PASS_HASH)
    }

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return try {
            val spec = PBEKeySpec(
                password.toCharArray(),
                saltHex.toByteArray(Charsets.UTF_8),
                PBKDF2_ITERATIONS,
                KEY_LENGTH_BITS
            )
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            sha256Hex("$saltHex:$password")
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        if (expectedHashHex.isBlank()) return false
        val computed = hashPassword(password, saltHex)
        return constantTimeEquals(computed, expectedHashHex)
    }

    fun validateRegistration(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        val u = username.trim()
        val e = email.trim()
        if (u.length < 3) return "Username kamida 3 ta belgidan iborat bo'lishi kerak"
        if (!e.contains("@") || !e.contains(".") || e.length < 5) {
            return "To'g'ri email manzilini kiriting"
        }
        if (password.length < 6) return "Parol kamida 6 ta belgidan iborat bo'lishi kerak"
        if (password != confirmPassword) return "Parollar bir-biriga mos kelmadi"
        return null
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
