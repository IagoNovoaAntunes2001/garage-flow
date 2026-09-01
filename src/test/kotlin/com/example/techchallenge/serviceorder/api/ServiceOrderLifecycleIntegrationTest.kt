package com.example.techchallenge.serviceorder.api

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "garage-flow.security.bootstrap.enabled=true",
    "garage-flow.security.bootstrap.username=order-admin",
    "garage-flow.security.bootstrap.password=SecurePassword123!",
])
class ServiceOrderLifecycleIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var token: String

    @BeforeEach
    fun prepare() {
        cleanup()
        token = issueToken()
    }

    @Test
    fun `creates approves executes tracks lists and measures service order`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val created = createServiceOrder(serviceId, partId, vehicleId)
        val orderId = created.first
        val trackingToken = created.second

        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("IN_DIAGNOSIS") }
        }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") { bearer(); json(); content = """{"items":[]}""" }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("AWAITING_APPROVAL") }
            jsonPath("$.serviceOrder.currentQuotation.total") { value("190.00") }
        }
        mockMvc.post("/api/v1/customer-approvals/$orderId/approve") {
            header("X-Service-Order-Token", trackingToken); json(); content = """{"quotationVersion":1}"""
        }.andExpect {
            status { isOk() }; jsonPath("$.status") { value("AWAITING_APPROVAL") }
        }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/execution/start") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("IN_EXECUTION") }
            jsonPath("$.serviceOrder.items[1].consumedQuantity") { value(2) }
        }
        mockMvc.get("/api/v1/admin/inventory-items/$partId") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.availableQuantity") { value(3) }
        }
        mockMvc.get("/api/v1/tracking/service-orders/$orderId") {
            header("X-Service-Order-Token", trackingToken)
        }.andExpect {
            status { isOk() }; jsonPath("$.status") { value("IN_EXECUTION") }
        }
        mockMvc.get("/api/v1/tracking/service-orders/$orderId") {
            header("X-Service-Order-Token", "wrong-token")
        }.andExpect { status { isNotFound() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/finish") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("FINISHED") }
        }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/delivery") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("DELIVERED") }
        }
        mockMvc.get("/api/v1/admin/service-orders") { bearer(); param("status", "DELIVERED") }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1))
        }
        mockMvc.get("/api/v1/admin/metrics/execution-time") {
            bearer(); param("from", "2026-08-31T00:00:00Z"); param("to", "2026-09-02T00:00:00Z")
        }.andExpect {
            status { isOk() }; jsonPath("$.orderCount") { value(1) }
        }
    }

    @Test
    fun `blocks execution while approval is pending and rolls back insufficient stock`() {
        val customerId = createCustomer("11144477735")
        val vehicleId = createVehicle(customerId, "DEF2G34")
        val serviceId = createService("Alignment", "90.00")
        val partId = createInventoryItem("PART", "Brake pad", 1)
        val (orderId, trackingToken) = createServiceOrder(serviceId, partId, vehicleId, customerDocument = "11144477735", partQuantity = 2)

        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") { bearer(); json(); content = """{"items":[]}""" }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/execution/start") { bearer() }.andExpect {
            status { isBadRequest() }; jsonPath("$.code") { value("QUOTATION_APPROVAL_REQUIRED") }
        }
        mockMvc.post("/api/v1/customer-approvals/$orderId/approve") {
            header("X-Service-Order-Token", trackingToken); json(); content = """{"quotationVersion":1}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/execution/start") { bearer() }.andExpect {
            status { isBadRequest() }; jsonPath("$.code") { value("INSUFFICIENT_STOCK") }
        }
        mockMvc.get("/api/v1/admin/inventory-items/$partId") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.availableQuantity") { value(1) }
        }
    }

    private fun createCustomer(document: String = "52998224725"): String {
        val body = mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"$document","name":"Maria Silva"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id")
    }

    private fun createVehicle(customerId: String, plate: String = "ABC1D23"): String {
        val body = mockMvc.post("/api/v1/admin/customers/$customerId/vehicles") {
            bearer(); json(); content = """{"licensePlate":"$plate","brand":"Ford","model":"Ka","year":2020}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id")
    }

    private fun createService(name: String = "Oil Change", price: String = "120.00"): String {
        val body = mockMvc.post("/api/v1/admin/services") {
            bearer(); json(); content = """{"name":"$name","description":"$name description","price":$price}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id")
    }

    private fun createInventoryItem(type: String = "PART", name: String = "Oil filter", quantity: Long = 5): String {
        val body = mockMvc.post("/api/v1/admin/inventory-items") {
            bearer(); json(); content = """{"type":"$type","name":"$name","description":"$name description","unitPrice":35.00,"availableQuantity":$quantity}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id")
    }

    private fun createServiceOrder(serviceId: String, partId: String, vehicleId: String, customerDocument: String = "52998224725", partQuantity: Long = 2): Pair<String, String> {
        val body = mockMvc.post("/api/v1/admin/service-orders") {
            bearer(); json()
            content = """{"customerDocument":"$customerDocument","vehicleId":"$vehicleId","items":[{"type":"SERVICE","referenceId":"$serviceId","quantity":1},{"type":"PART","referenceId":"$partId","quantity":$partQuantity}]}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id") to extract(body, "trackingToken")
    }

    private fun issueToken(): String {
        val body = mockMvc.post("/api/v1/auth/token") {
            json(); content = """{"username":"order-admin","password":"SecurePassword123!"}"""
        }.andReturn().response.contentAsString
        return extract(body, "accessToken")
    }

    private fun cleanup() {
        listOf(
            "inventory_movements", "approvals", "quotation_lines", "quotations", "service_order_status_history",
            "service_order_items", "service_orders", "inventory_items", "catalog_services", "vehicles", "customers",
        ).forEach { jdbcTemplate.update("DELETE FROM $it") }
    }

    private fun extract(body: String, field: String): String = Regex(""""$field":"([^"]+)"""").find(body)!!.groupValues[1]
    private fun MockHttpServletRequestDsl.bearer() = header("Authorization", "Bearer $token")
    private fun MockHttpServletRequestDsl.json() { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
}
