package com.example.techchallenge.shared.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
@TestPropertySource(properties = ["springdoc.api-docs.enabled=true"])
class OpenApiContractIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `publishes OpenAPI metadata and bearer scheme`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.components.securitySchemes.bearerAuth.scheme") { value("bearer") }
        }
    }
}
