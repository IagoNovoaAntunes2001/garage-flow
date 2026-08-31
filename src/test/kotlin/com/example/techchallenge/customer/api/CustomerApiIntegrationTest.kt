package com.example.techchallenge.customer.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "garage-flow.security.bootstrap.enabled=true",
    "garage-flow.security.bootstrap.username=customer-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class CustomerApiIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var token: String

    @BeforeEach
    fun prepare() {
        jdbcTemplate.update("DELETE FROM vehicles")
        jdbcTemplate.update("DELETE FROM customers")
        token = issueToken()
    }

    @Test
    fun `requires administrator JWT for customer operations`() {
        mockMvc.get("/api/v1/admin/customers").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `creates retrieves updates lists and removes an unreferenced customer`() {
        val id = createCustomer("529.982.247-25", "Maria Silva")

        mockMvc.get("/api/v1/admin/customers/$id") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.document") { value("52998224725") }
        }
        mockMvc.get("/api/v1/admin/customers") { bearer(); param("document", "529.982.247-25") }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1)); jsonPath("$.totalElements") { value(1) }
        }
        mockMvc.put("/api/v1/admin/customers/$id") {
            bearer(); json(); content = """{"name":"Maria Souza","email":"maria@example.com","active":true}"""
        }.andExpect { status { isOk() }; jsonPath("$.name") { value("Maria Souza") } }
        mockMvc.delete("/api/v1/admin/customers/$id") { bearer() }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v1/admin/customers/$id") { bearer() }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `rejects duplicate normalized document and invalid input`() {
        createCustomer("529.982.247-25", "Maria Silva")
        mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"52998224725","name":"Outra Pessoa"}"""
        }.andExpect { status { isConflict() }; jsonPath("$.code") { value("CONFLICT") } }
        mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"11111111111","name":"x"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `deactivates rather than deleting a referenced customer`() {
        val id = createCustomer("52998224725", "Maria Silva")
        jdbcTemplate.update(
            "INSERT INTO vehicles(id, customer_id, license_plate, brand, model, production_year, active, created_at, updated_at, version) VALUES (?, ?, 'ABC1D23', 'Ford', 'Ka', 2020, true, now(), now(), 0)",
            UUID.randomUUID(), UUID.fromString(id),
        )

        mockMvc.delete("/api/v1/admin/customers/$id") { bearer() }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v1/admin/customers/$id") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.active") { value(false) }
        }
    }

    private fun createCustomer(document: String, name: String): String {
        val response = mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"$document","name":"$name"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return Regex("\"id\":\"([^\"]+)\"").find(response)!!.groupValues[1]
    }

    private fun issueToken(): String {
        val response = mockMvc.post("/api/v1/auth/token") {
            json(); content = """{"username":"customer-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(response)!!.groupValues[1]
    }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer() =
        header("Authorization", "Bearer $token")

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.json() {
        contentType = org.springframework.http.MediaType.APPLICATION_JSON
    }
}
