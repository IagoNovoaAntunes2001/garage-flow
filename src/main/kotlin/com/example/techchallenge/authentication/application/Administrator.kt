package com.example.techchallenge.authentication.application

import com.example.techchallenge.shared.domain.AdministratorId
import java.time.Instant

enum class AdministratorRole { ADMIN }

class Administrator private constructor(
    val id: AdministratorId,
    val username: String,
    val passwordHash: String,
    val active: Boolean,
    val roles: Set<AdministratorRole>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    fun deactivate(now: Instant): Administrator = Administrator(
        id, username, passwordHash, false, roles, createdAt, now, version,
    )

    companion object {
        fun create(
            id: AdministratorId,
            username: String,
            passwordHash: String,
            now: Instant,
        ): Administrator {
            val normalizedUsername = normalizeUsername(username)
            require(normalizedUsername.isNotBlank()) { "Administrator username is required" }
            require(passwordHash.isNotBlank()) { "Administrator password hash is required" }
            return Administrator(
                id = id,
                username = normalizedUsername,
                passwordHash = passwordHash,
                active = true,
                roles = setOf(AdministratorRole.ADMIN),
                createdAt = now,
                updatedAt = now,
                version = 0,
            )
        }

        fun restore(
            id: AdministratorId,
            username: String,
            passwordHash: String,
            active: Boolean,
            roles: Set<AdministratorRole>,
            createdAt: Instant,
            updatedAt: Instant,
            version: Long,
        ) = Administrator(id, normalizeUsername(username), passwordHash, active, roles, createdAt, updatedAt, version)

        fun normalizeUsername(username: String): String = username.trim().lowercase()
    }
}

interface AdministratorRepository {
    fun findByUsername(username: String): Administrator?
    fun existsByUsername(username: String): Boolean
    fun count(): Long
    fun save(administrator: Administrator): Administrator
}

fun interface AccessTokenIssuer {
    fun issue(administrator: Administrator): IssuedAccessToken
}

data class IssuedAccessToken(
    val accessToken: String,
    val expiresIn: Long,
)
