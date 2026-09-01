package com.example.techchallenge.inventory.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "garage-flow.security.bootstrap.enabled=true",
    "garage-flow.security.bootstrap.username=inventory-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class InventoryApiIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var token: String

    @BeforeEach
    fun prepare() {
        jdbcTemplate.update("DELETE FROM inventory_movements")
        jdbcTemplate.update("DELETE FROM inventory_items")
        token = token()
    }

    @Test
    fun `creates retrieves updates lists adjusts and removes inventory item`() {
        val id = createItem("PART", "Oil filter", 3)
        mockMvc.get("/api/v1/admin/inventory-items/$id") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.availableQuantity") { value(3) }
        }
        mockMvc.post("/api/v1/admin/inventory-items/$id/stock-adjustments") {
            bearer(); json(); content = """{"operation":"ADD","quantity":7,"reason":"purchase"}"""
        }.andExpect { status { isOk() }; jsonPath("$.availableQuantity") { value(10) } }
        mockMvc.post("/api/v1/admin/inventory-items/$id/stock-adjustments") {
            bearer(); json(); content = """{"operation":"REMOVE","quantity":4,"reason":"inventory correction"}"""
        }.andExpect { status { isOk() }; jsonPath("$.availableQuantity") { value(6) } }
        mockMvc.put("/api/v1/admin/inventory-items/$id") {
            bearer(); json(); content = """{"type":"PART","name":"Premium oil filter","description":"Premium oil filter","unitPrice":39.90,"active":true}"""
        }.andExpect { status { isOk() }; jsonPath("$.name") { value("Premium oil filter") } }
        mockMvc.get("/api/v1/admin/inventory-items") { bearer(); param("type", "PART") }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1))
        }
        mockMvc.delete("/api/v1/admin/inventory-items/$id") { bearer() }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v1/admin/inventory-items/$id") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.active") { value(false) }
        }
    }

    @Test
    fun `rejects duplicate active item and insufficient stock`() {
        val id = createItem("SUPPLY", "Brake fluid", 1)
        mockMvc.post("/api/v1/admin/inventory-items") {
            bearer(); json(); content = itemJson("SUPPLY", "brake fluid", 4)
        }.andExpect { status { isConflict() } }
        mockMvc.post("/api/v1/admin/inventory-items/$id/stock-adjustments") {
            bearer(); json(); content = """{"operation":"REMOVE","quantity":2,"reason":"correction"}"""
        }.andExpect { status { isBadRequest() }; jsonPath("$.code") { value("INSUFFICIENT_STOCK") } }
    }

    private fun createItem(type: String, name: String, quantity: Long): String {
        val body = mockMvc.post("/api/v1/admin/inventory-items") {
            bearer(); json(); content = itemJson(type, name, quantity)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun token(): String {
        val body = mockMvc.post("/api/v1/auth/token") {
            json(); content = """{"username":"inventory-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun itemJson(type: String, name: String, quantity: Long) =
        """{"type":"$type","name":"$name","description":"$name description","unitPrice":25.00,"availableQuantity":$quantity}"""

    private fun MockHttpServletRequestDsl.bearer() = header("Authorization", "Bearer $token")
    private fun MockHttpServletRequestDsl.json() { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
}
