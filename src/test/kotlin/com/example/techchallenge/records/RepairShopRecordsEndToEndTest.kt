package com.example.techchallenge.records

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class RepairShopRecordsEndToEndTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `records APIs are protected through the administrative boundary`() {
        mockMvc.get("/api/v1/admin/customers").andExpect {
            status { isUnauthorized() }
        }
        mockMvc.get("/api/v1/admin/vehicles").andExpect {
            status { isUnauthorized() }
        }
    }
}
