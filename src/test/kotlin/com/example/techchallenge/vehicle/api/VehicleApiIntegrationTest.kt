package com.example.techchallenge.vehicle.api

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
    "garage-flow.security.bootstrap.username=vehicle-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class VehicleApiIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var token: String
    private lateinit var customerId: String

    @BeforeEach
    fun prepare() {
        jdbcTemplate.update("DELETE FROM vehicles")
        jdbcTemplate.update("DELETE FROM customers")
        token = token()
        customerId = createCustomer()
    }

    @Test
    fun `creates retrieves updates lists and removes vehicle`() {
        val vehicleId = createVehicle("abc-1d23")
        mockMvc.get("/api/v1/admin/vehicles/$vehicleId") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.licensePlate") { value("ABC1D23") }; jsonPath("$.customerId") { value(customerId) }
        }
        mockMvc.put("/api/v1/admin/vehicles/$vehicleId") {
            bearer(); json(); content = """{"licensePlate":"DEF2G34","brand":"Honda","model":"Fit","year":2021}"""
        }.andExpect { status { isOk() }; jsonPath("$.brand") { value("Honda") } }
        mockMvc.get("/api/v1/admin/customers/$customerId/vehicles") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1))
        }
        mockMvc.get("/api/v1/admin/vehicles") { bearer(); param("licensePlate", "def-2g34") }.andExpect {
            status { isOk() }; jsonPath("$.totalElements") { value(1) }
        }
        mockMvc.delete("/api/v1/admin/vehicles/$vehicleId") { bearer() }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v1/admin/vehicles/$vehicleId") { bearer() }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `rejects duplicate plate and invalid year`() {
        createVehicle("ABC1D23")
        mockMvc.post("/api/v1/admin/customers/$customerId/vehicles") {
            bearer(); json(); content = vehicleJson("abc-1d23")
        }.andExpect { status { isConflict() }; jsonPath("$.code") { value("CONFLICT") } }
        mockMvc.post("/api/v1/admin/customers/$customerId/vehicles") {
            bearer(); json(); content = """{"licensePlate":"DEF2G34","brand":"Ford","model":"Ka","year":3000}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `rejects missing or inactive customer ownership`() {
        mockMvc.post("/api/v1/admin/customers/00000000-0000-0000-0000-000000000001/vehicles") {
            bearer(); json(); content = vehicleJson("ABC1D23")
        }.andExpect { status { isNotFound() } }
        jdbcTemplate.update("UPDATE customers SET active=false WHERE id=?::uuid", customerId)
        mockMvc.post("/api/v1/admin/customers/$customerId/vehicles") {
            bearer(); json(); content = vehicleJson("ABC1D23")
        }.andExpect { status { isUnprocessableEntity() } }
    }

    private fun createVehicle(plate: String): String {
        val body = mockMvc.post("/api/v1/admin/customers/$customerId/vehicles") {
            bearer(); json(); content = vehicleJson(plate)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun createCustomer(): String {
        val body = mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"52998224725","name":"Maria Silva"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun token(): String {
        val body = mockMvc.post("/api/v1/auth/token") {
            json(); content = """{"username":"vehicle-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun vehicleJson(plate: String) = """{"licensePlate":"$plate","brand":"Ford","model":"Ka","year":2020}"""
    private fun MockHttpServletRequestDsl.bearer() = header("Authorization", "Bearer $token")
    private fun MockHttpServletRequestDsl.json() { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
}
