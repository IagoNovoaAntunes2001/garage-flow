package com.example.techchallenge.support

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class RepairShopMvpEndToEndTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `exposes public health while keeping administrative APIs protected`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
        }
        mockMvc.get("/api/v1/admin/services").andExpect {
            status { isUnauthorized() }
        }
    }
}
