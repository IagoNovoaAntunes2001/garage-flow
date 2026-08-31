package com.example.techchallenge.authentication.application

import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.ClockProvider
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val repository: AdministratorRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenIssuer: AccessTokenIssuer,
) {
    private val dummyPasswordHash = passwordEncoder.encode("garage-flow-invalid-credential")

    fun authenticate(username: String, password: String): IssuedAccessToken {
        val administrator = repository.findByUsername(Administrator.normalizeUsername(username))
        val passwordMatches = passwordEncoder.matches(password, administrator?.passwordHash ?: dummyPasswordHash)
        if (administrator == null || !administrator.active || !passwordMatches) {
            throw BadCredentialsException(INVALID_CREDENTIALS)
        }
        return tokenIssuer.issue(administrator)
    }

    companion object {
        const val INVALID_CREDENTIALS = "Invalid credentials"
    }
}

@ConfigurationProperties("garage-flow.security.bootstrap")
data class AdministratorBootstrapProperties(
    val enabled: Boolean = false,
    val username: String = "",
    val password: String = "",
)

@Component
class AdministratorBootstrap(
    private val properties: AdministratorBootstrapProperties,
    private val repository: AdministratorRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clockProvider: ClockProvider,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (!properties.enabled) return
        require(properties.username.isNotBlank()) { "ADMIN_USERNAME is required when bootstrap is enabled" }
        require(properties.password.length >= 12) { "ADMIN_PASSWORD must contain at least 12 characters" }
        if (repository.existsByUsername(properties.username)) return

        repository.save(
            Administrator.create(
                id = AdministratorId.new(),
                username = properties.username,
                passwordHash = passwordEncoder.encode(properties.password),
                now = clockProvider.now(),
            ),
        )
    }
}
