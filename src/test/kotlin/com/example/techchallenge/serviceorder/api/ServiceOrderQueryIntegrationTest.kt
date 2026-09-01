package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class ServiceOrderQueryIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `requires administrator authentication for service order listing`() {
        mockMvc.get("/api/v1/admin/service-orders").andExpect {
            status { isUnauthorized() }
        }
    }
}
