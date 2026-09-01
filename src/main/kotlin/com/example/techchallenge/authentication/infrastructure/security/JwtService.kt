package com.example.techchallenge.authentication.infrastructure.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.example.techchallenge.authentication.application.AccessTokenIssuer
import com.example.techchallenge.authentication.application.Administrator
import com.example.techchallenge.authentication.application.IssuedAccessToken
import com.example.techchallenge.shared.domain.ClockProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties("garage-flow.security.jwt")
data class JwtProperties(
    val issuer: String = "garage-flow",
    val secret: String = "",
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
)

class JwtService(
    private val properties: JwtProperties,
    private val clockProvider: ClockProvider,
) : AccessTokenIssuer {
    private val secretKey = SecretKeySpec(validatedSecret(), "HmacSHA256")
    private val encoder: JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey))
    private val jwtDecoder: JwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    init {
        require(properties.issuer.isNotBlank()) { "JWT issuer must be configured" }
        require(!properties.accessTokenTtl.isZero && !properties.accessTokenTtl.isNegative) {
            "JWT access token TTL must be positive"
        }
    }

    override fun issue(administrator: Administrator): IssuedAccessToken {
        val issuedAt = clockProvider.now()
        val expiresAt = issuedAt.plus(properties.accessTokenTtl)
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(administrator.username)
            .claim("administratorId", administrator.id.value.toString())
            .claim("roles", administrator.roles.map { it.name }.sorted())
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedAccessToken(token, properties.accessTokenTtl.seconds)
    }

    fun decoder(): JwtDecoder = jwtDecoder

    private fun validatedSecret(): ByteArray = properties.secret.toByteArray(StandardCharsets.UTF_8).also {
        require(it.size >= 32) { "JWT secret must contain at least 32 bytes" }
    }
}
