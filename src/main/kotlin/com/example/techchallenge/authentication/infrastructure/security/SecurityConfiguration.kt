package com.example.techchallenge.authentication.infrastructure.security

import com.example.techchallenge.authentication.application.AdministratorBootstrapProperties
import com.example.techchallenge.shared.api.error.ApiError
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.infrastructure.observability.CorrelationIdFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class, AdministratorBootstrapProperties::class)
class SecurityConfiguration {
    @Bean
    fun clockProvider(): ClockProvider = ClockProvider(Instant::now)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtService(properties: JwtProperties, clockProvider: ClockProvider) = JwtService(properties, clockProvider)

    @Bean
    fun jwtDecoder(jwtService: JwtService): JwtDecoder = jwtService.decoder()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationEntryPoint: AuthenticationEntryPoint,
        accessDeniedHandler: AccessDeniedHandler,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(
                "/api/v1/auth/token",
                "/api/v1/tracking/**",
                "/api/v1/customer-approvals/**",
                "/actuator/health",
                "/actuator/health/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
            ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll()
        }
        .oauth2ResourceServer {
            it.authenticationEntryPoint(authenticationEntryPoint)
            it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
        }
        .exceptionHandling {
            it.authenticationEntryPoint(authenticationEntryPoint)
            it.accessDeniedHandler(accessDeniedHandler)
        }
        .build()

    @Bean
    fun authenticationEntryPoint(objectMapper: ObjectMapper): AuthenticationEntryPoint =
        JsonSecurityErrorWriter(objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required")

    @Bean
    fun accessDeniedHandler(objectMapper: ObjectMapper): AccessDeniedHandler =
        JsonSecurityErrorWriter(objectMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied")

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authorities = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("roles")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authorities) }
    }
}

private class JsonSecurityErrorWriter(
    private val objectMapper: ObjectMapper,
    private val status: HttpStatus,
    private val code: String,
    private val message: String,
) : AuthenticationEntryPoint, AccessDeniedHandler {
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException) =
        write(request, response)

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, exception: org.springframework.security.access.AccessDeniedException) =
        write(request, response)

    private fun write(request: HttpServletRequest, response: HttpServletResponse) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            ApiError(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                code = code,
                message = message,
                path = request.requestURI,
                correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME)?.toString() ?: "unavailable",
            ),
        )
    }
}
