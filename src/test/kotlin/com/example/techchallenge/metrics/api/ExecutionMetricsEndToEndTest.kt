package com.example.techchallenge.metrics.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "garage-flow.security.bootstrap.enabled=true",
    "garage-flow.security.bootstrap.username=metrics-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class ExecutionMetricsEndToEndTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `requires authentication for execution metrics`() {
        mockMvc.get("/api/v1/admin/metrics/execution-time") {
            param("from", "2026-08-31T00:00:00Z")
            param("to", "2026-09-01T00:00:00Z")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `rejects invalid execution metrics date range`() {
        val token = issueToken()

        mockMvc.get("/api/v1/admin/metrics/execution-time") {
            header("Authorization", "Bearer $token")
            param("from", "2026-09-01T00:00:00Z")
            param("to", "2026-08-31T00:00:00Z")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PAGINATION") }
        }
    }

    private fun issueToken(): String {
        val body = mockMvc.post("/api/v1/auth/token") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"username":"metrics-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return Regex(""""accessToken":"([^"]+)"""").find(body)!!.groupValues[1]
    }
}
