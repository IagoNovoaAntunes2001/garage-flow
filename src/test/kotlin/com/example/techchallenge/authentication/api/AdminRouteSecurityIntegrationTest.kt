package com.example.techchallenge.authentication.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@AutoConfigureMockMvc
@Import(AdminRouteSecurityIntegrationTest.ProbeConfiguration::class)
@TestPropertySource(
    properties = [
        "garage-flow.security.bootstrap.enabled=true",
        "garage-flow.security.bootstrap.username=route-admin",
        "garage-flow.security.bootstrap.password=SecurePassword123!",
    ],
)
class AdminRouteSecurityIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `all administrative routes reject missing and invalid JWTs`() {
        mockMvc.get("/api/v1/admin/security-probe").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }

        mockMvc.get("/api/v1/admin/security-probe") {
            header("Authorization", "Bearer invalid.jwt.value")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `administrator JWT grants access only to protected administrative routes`() {
        val response = mockMvc.post("/api/v1/auth/token") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"username":"route-admin","password":"SecurePassword123!"}"""
        }.andReturn().response
        val accessToken = Regex("\"accessToken\":\"([^\"]+)\"").find(response.contentAsString)!!.groupValues[1]

        mockMvc.get("/api/v1/admin/security-probe") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            content { string("secured") }
        }
    }

    @TestConfiguration
    class ProbeConfiguration {
        @Bean
        fun adminSecurityProbe() = AdminSecurityProbe()
    }

    @RestController
    class AdminSecurityProbe {
        @GetMapping("/api/v1/admin/security-probe")
        fun probe() = "secured"
    }
}
