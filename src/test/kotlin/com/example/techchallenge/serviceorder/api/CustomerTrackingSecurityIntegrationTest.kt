package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@AutoConfigureMockMvc
class CustomerTrackingSecurityIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `conceals unknown tracking identifiers instead of exposing service order existence details`() {
        mockMvc.get("/api/v1/tracking/service-orders/${UUID.randomUUID()}") {
            header("X-Service-Order-Token", "wrong-token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
        }
    }
}
