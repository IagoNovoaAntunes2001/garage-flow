package com.example.techchallenge.authentication.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "garage-flow.security.bootstrap.enabled=true",
        "garage-flow.security.bootstrap.username=workshop-admin",
        "garage-flow.security.bootstrap.password=SecurePassword123!",
    ],
)
class AuthenticationIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `issues a bearer token for bootstrapped administrator`() {
        mockMvc.post("/api/v1/auth/token") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"username":"workshop-admin","password":"SecurePassword123!"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(greaterThan(0)) }
            jsonPath("$.password") { doesNotExist() }
        }
    }

    @Test
    fun `rejects invalid credentials without identifying the failed field`() {
        mockMvc.post("/api/v1/auth/token") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"username":"workshop-admin","password":"wrong"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("Invalid credentials") }
            jsonPath("$.stackTrace") { doesNotExist() }
        }
    }
}
