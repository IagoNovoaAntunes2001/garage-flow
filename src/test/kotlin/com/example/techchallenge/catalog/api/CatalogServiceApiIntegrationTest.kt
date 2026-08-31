package com.example.techchallenge.catalog.api

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
    "garage-flow.security.bootstrap.username=catalog-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class CatalogServiceApiIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var token: String

    @BeforeEach
    fun prepare() {
        jdbcTemplate.update("DELETE FROM catalog_services")
        token = token()
    }

    @Test
    fun `requires administrator JWT`() {
        mockMvc.get("/api/v1/admin/services").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `creates retrieves updates lists and removes a service`() {
        val id = createService("Oil Change")
        mockMvc.get("/api/v1/admin/services/$id") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.price") { value("120.00") }
        }
        mockMvc.put("/api/v1/admin/services/$id") {
            bearer(); json(); content = """{"name":"Oil and filter","description":"Oil and filter replacement","price":180.50,"active":true}"""
        }.andExpect { status { isOk() }; jsonPath("$.name") { value("Oil and filter") } }
        mockMvc.get("/api/v1/admin/services") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1))
        }
        mockMvc.delete("/api/v1/admin/services/$id") { bearer() }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v1/admin/services/$id") { bearer() }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `rejects duplicate active name and invalid price`() {
        createService("Alignment")
        mockMvc.post("/api/v1/admin/services") {
            bearer(); json(); content = serviceJson("alignment", "Wheel alignment", "90.00")
        }.andExpect { status { isConflict() } }
        mockMvc.post("/api/v1/admin/services") {
            bearer(); json(); content = serviceJson("T", "Tiny", "-1.00")
        }.andExpect { status { isBadRequest() } }
    }

    private fun createService(name: String): String {
        val body = mockMvc.post("/api/v1/admin/services") {
            bearer(); json(); content = serviceJson(name, "Engine oil replacement", "120.00")
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun token(): String {
        val body = mockMvc.post("/api/v1/auth/token") {
            json(); content = """{"username":"catalog-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun serviceJson(name: String, description: String, price: String) =
        """{"name":"$name","description":"$description","price":$price}"""

    private fun MockHttpServletRequestDsl.bearer() = header("Authorization", "Bearer $token")
    private fun MockHttpServletRequestDsl.json() { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
}
