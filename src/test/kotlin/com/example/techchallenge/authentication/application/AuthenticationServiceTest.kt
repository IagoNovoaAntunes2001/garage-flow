package com.example.techchallenge.authentication.application

import com.example.techchallenge.authentication.infrastructure.security.JwtProperties
import com.example.techchallenge.authentication.infrastructure.security.JwtService
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.ClockProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Duration
import java.time.Instant

class AuthenticationServiceTest {
    private val passwordEncoder = BCryptPasswordEncoder()
    private val administrator = Administrator.create(
        id = AdministratorId.new(),
        username = "workshop-admin",
        passwordHash = passwordEncoder.encode("correct-password"),
        now = Instant.parse("2026-08-31T12:00:00Z"),
    )

    @Test
    fun `issues a token for valid active administrator credentials`() {
        val tokenIssuer = RecordingTokenIssuer()
        val service = AuthenticationService(
            repository = InMemoryAdministratorRepository(administrator),
            passwordEncoder = passwordEncoder,
            tokenIssuer = tokenIssuer,
        )

        val result = service.authenticate(" workshop-admin ", "correct-password")

        assertThat(result.accessToken).isEqualTo("signed-token")
        assertThat(result.expiresIn).isEqualTo(900)
        assertThat(tokenIssuer.issuedFor).isEqualTo(administrator)
    }

    @Test
    fun `uses the same generic failure for unknown wrong-password and inactive users`() {
        val activeService = AuthenticationService(
            InMemoryAdministratorRepository(administrator),
            passwordEncoder,
            RecordingTokenIssuer(),
        )
        val inactiveService = AuthenticationService(
            InMemoryAdministratorRepository(administrator.deactivate(Instant.parse("2026-08-31T12:01:00Z"))),
            passwordEncoder,
            RecordingTokenIssuer(),
        )

        listOf(
            { activeService.authenticate("unknown", "correct-password") },
            { activeService.authenticate("workshop-admin", "wrong-password") },
            { inactiveService.authenticate("workshop-admin", "correct-password") },
        ).forEach { attempt ->
            assertThatThrownBy { attempt(); Unit }
                .isInstanceOf(BadCredentialsException::class.java)
                .hasMessage("Invalid credentials")
        }
    }

    @Test
    fun `JWT contains configured issuer expiry subject and administrator authority`() {
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val jwtService = JwtService(
            properties = JwtProperties(
                issuer = "garage-flow-test",
                secret = "01234567890123456789012345678901",
                accessTokenTtl = Duration.ofMinutes(15),
            ),
            clockProvider = ClockProvider { now },
        )

        val issued = jwtService.issue(administrator)
        val decoded = jwtService.decoder().decode(issued.accessToken)

        assertThat(decoded.getClaimAsString("iss")).isEqualTo("garage-flow-test")
        assertThat(decoded.subject).isEqualTo("workshop-admin")
        assertThat(decoded.issuedAt).isEqualTo(now)
        assertThat(decoded.expiresAt).isEqualTo(now.plusSeconds(900))
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("ADMIN")
    }

    private class RecordingTokenIssuer : AccessTokenIssuer {
        var issuedFor: Administrator? = null

        override fun issue(administrator: Administrator): IssuedAccessToken {
            issuedFor = administrator
            return IssuedAccessToken("signed-token", 900)
        }
    }

    private class InMemoryAdministratorRepository(
        administrator: Administrator? = null,
    ) : AdministratorRepository {
        private val values = administrator?.let { mutableListOf(it) } ?: mutableListOf()

        override fun findByUsername(username: String): Administrator? = values.singleOrNull { it.username == username }

        override fun existsByUsername(username: String): Boolean = findByUsername(username) != null

        override fun count(): Long = values.size.toLong()

        override fun save(administrator: Administrator): Administrator {
            values.removeIf { it.id == administrator.id }
            values += administrator
            return administrator
        }
    }
}
