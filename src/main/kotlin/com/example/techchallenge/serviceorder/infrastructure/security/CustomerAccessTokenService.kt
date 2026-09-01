package com.example.techchallenge.serviceorder.infrastructure.security

import com.example.techchallenge.serviceorder.application.port.CustomerAccessToken
import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Component
class CustomerAccessTokenService : CustomerAccessTokenPort {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun issueToken(): CustomerAccessToken {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val raw = encoder.encodeToString(bytes)
        return CustomerAccessToken(raw, hash(raw))
    }

    override fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
