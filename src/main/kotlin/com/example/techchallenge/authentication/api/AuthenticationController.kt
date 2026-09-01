package com.example.techchallenge.authentication.api

import com.example.techchallenge.authentication.application.AuthenticationService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AuthenticationRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
)

data class AuthenticationResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService,
) {
    @PostMapping("/token")
    fun token(@Valid @RequestBody request: AuthenticationRequest): ResponseEntity<AuthenticationResponse> {
        val token = authenticationService.authenticate(request.username, request.password)
        return ResponseEntity.ok(AuthenticationResponse(token.accessToken, expiresIn = token.expiresIn))
    }
}
