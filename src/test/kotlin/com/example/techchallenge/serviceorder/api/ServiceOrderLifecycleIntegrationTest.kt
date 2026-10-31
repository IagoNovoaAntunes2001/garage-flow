package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.SimpleMailMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.example.techchallenge.serviceorder.infrastructure.notification.StatusNotificationWorker
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
    "garage-flow.integration.quotation-secret=integration-test-secret-at-least-32-characters",
])
class ServiceOrderLifecycleIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    @Autowired private lateinit var transactionManager: PlatformTransactionManager
    private lateinit var token: String

    @BeforeEach
    fun prepare() {
        cleanup()
        token = issueToken()
    }

    @Test
    fun `creates approves executes tracks lists and measures service order`() {
        val periodStart = java.time.Instant.now().minusSeconds(60)
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val created = createServiceOrder(serviceId, partId, vehicleId)
        val orderId = created.first
        val trackingToken = created.second

        assertTrackingStatus(orderId, trackingToken, "RECEIVED")
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("IN_DIAGNOSIS") }
        }
        assertTrackingStatus(orderId, trackingToken, "IN_DIAGNOSIS")
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") { bearer(); json(); content = """{"items":[]}""" }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("AWAITING_APPROVAL") }
            jsonPath("$.serviceOrder.currentQuotation.total") { value("190.00") }
        }
        assertTrackingStatus(orderId, trackingToken, "AWAITING_APPROVAL")
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
        assertTrackingStatus(orderId, trackingToken, "FINISHED")
        mockMvc.post("/api/v1/admin/service-orders/$orderId/delivery") { bearer() }.andExpect {
            status { isOk() }; jsonPath("$.serviceOrder.status") { value("DELIVERED") }
        }
        assertTrackingStatus(orderId, trackingToken, "DELIVERED")
        mockMvc.get("/api/v1/admin/service-orders") { bearer(); param("status", "DELIVERED") }.andExpect {
            status { isOk() }; jsonPath("$.content", hasSize<Any>(1))
        }
        mockMvc.get("/api/v1/admin/metrics/execution-time") {
            bearer(); param("from", periodStart.toString()); param("to", java.time.Instant.now().plusSeconds(60).toString())
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

    @Test
    fun `rejects order for another customers vehicle without creating partial records`() {
        createCustomer()
        val otherCustomerId = createCustomer("11144477735")
        val vehicleId = createVehicle(otherCustomerId)
        val serviceId = createService()

        mockMvc.post("/api/v1/admin/service-orders") {
            bearer(); json()
            content = """{"customerDocument":"52998224725","vehicleId":"$vehicleId","items":[{"type":"SERVICE","referenceId":"$serviceId","quantity":1}]}"""
        }.andExpect { status { is4xxClientError() } }

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM service_orders", Int::class.java)!!)
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM service_order_items", Int::class.java)!!)
    }

    @Test
    fun `external quotation decision authenticates and deduplicates events`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") {
            bearer(); json(); content = """{"items":[]}"""
        }.andExpect { status { isOk() } }
        val payload = """{"eventId":"event-123","serviceOrderId":"$orderId","quotationVersion":1,"decision":"APPROVED"}"""

        mockMvc.post("/api/v1/integrations/quotation-decisions") { json(); content = payload }.andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/v1/integrations/quotation-decisions") {
            header("X-Integration-Token", "invalid-token"); json(); content = payload
        }.andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/v1/integrations/quotation-decisions") {
            header("X-Integration-Token", "integration-test-secret-at-least-32-characters"); json()
            content = payload.replace("\"quotationVersion\":1", "\"quotationVersion\":2")
        }.andExpect { status { isConflict() } }
        repeat(2) {
            mockMvc.post("/api/v1/integrations/quotation-decisions") {
                header("X-Integration-Token", "integration-test-secret-at-least-32-characters"); json(); content = payload
            }.andExpect { status { isOk() } }
        }
        mockMvc.post("/api/v1/integrations/quotation-decisions") {
            header("X-Integration-Token", "integration-test-secret-at-least-32-characters"); json()
            content = payload.replace("APPROVED", "REJECTED")
        }.andExpect { status { isConflict() } }
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approvals", Int::class.java)!!)
    }

    @Test
    fun `external rejection prevents execution`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") {
            bearer(); json(); content = """{"items":[]}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/integrations/quotation-decisions") {
            header("X-Integration-Token", "integration-test-secret-at-least-32-characters"); json()
            content = """{"eventId":"rejection-1","serviceOrderId":"$orderId","quotationVersion":1,"decision":"REJECTED","reason":"Not approved"}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/execution/start") { bearer() }.andExpect { status { isBadRequest() } }
        org.junit.jupiter.api.Assertions.assertEquals("EXTERNAL_INTEGRATION", jdbcTemplate.queryForObject("SELECT channel FROM approvals", String::class.java))
    }

    @Test
    fun `concurrent external retries produce one approval`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/quotation/request-approval") {
            bearer(); json(); content = """{"items":[]}"""
        }.andExpect { status { isOk() } }
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        val start = java.util.concurrent.CountDownLatch(1)
        try {
            val responses = (1..2).map {
                executor.submit<Int> {
                    start.await()
                    mockMvc.post("/api/v1/integrations/quotation-decisions") {
                        header("X-Integration-Token", "integration-test-secret-at-least-32-characters"); json()
                        content = """{"eventId":"concurrent-1","serviceOrderId":"$orderId","quotationVersion":1,"decision":"APPROVED"}"""
                    }.andReturn().response.status
                }
            }
            start.countDown()
            responses.forEach { org.junit.jupiter.api.Assertions.assertEquals(200, it.get()) }
            org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approvals", Int::class.java)!!)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `operational listing orders before pagination and excludes closed orders`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val statuses = listOf("RECEIVED", "IN_DIAGNOSIS", "AWAITING_APPROVAL", "IN_EXECUTION", "FINISHED", "DELIVERED")
        val orders = statuses.mapIndexed { index, state ->
            val orderId = createServiceOrder(serviceId, partId, vehicleId).first
            jdbcTemplate.update("UPDATE service_orders SET status = ?, created_at = ? WHERE id = ?", state,
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index.toLong())), java.util.UUID.fromString(orderId))
            orderId
        }
        val olderExecution = createServiceOrder(serviceId, partId, vehicleId).first
        jdbcTemplate.update("UPDATE service_orders SET status = ?, created_at = ? WHERE id = ?", "IN_EXECUTION",
            java.sql.Timestamp.from(java.time.Instant.parse("2025-01-01T00:00:00Z")), java.util.UUID.fromString(olderExecution))
        mockMvc.get("/api/v1/admin/service-orders/active") { bearer(); param("page", "0"); param("size", "2") }.andExpect {
            status { isOk() }; jsonPath("$.totalElements") { value(5) }
            jsonPath("$.content[0].id") { value(olderExecution) }; jsonPath("$.content[1].id") { value(orders[3]) }
        }
        mockMvc.get("/api/v1/admin/service-orders") { bearer(); param("page", "0"); param("size", "2") }.andExpect {
            status { isOk() }; jsonPath("$.totalElements") { value(5) }
            jsonPath("$.content[0].id") { value(olderExecution) }; jsonPath("$.content[1].id") { value(orders[3]) }
        }
        mockMvc.get("/api/v1/admin/service-orders/active") { bearer(); param("page", "1"); param("size", "2") }.andExpect {
            status { isOk() }; jsonPath("$.content[0].id") { value(orders[2]) }; jsonPath("$.content[1].id") { value(orders[1]) }
        }
        mockMvc.get("/api/v1/admin/service-orders/active") { bearer(); param("page", "2"); param("size", "2") }.andExpect {
            status { isOk() }; jsonPath("$.content[0].id") { value(orders[0]) }
        }
    }

    @Test
    fun `status transition records notification atomically`() {
        val customerId = createCustomer(email = "customer@example.com")
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/admin/service-orders/$orderId/diagnosis/start") { bearer() }.andExpect { status { is4xxClientError() } }
        val rows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM status_notifications WHERE service_order_id = ? AND status = 'IN_DIAGNOSIS' AND recipient = 'customer@example.com'",
            Int::class.java, java.util.UUID.fromString(orderId),
        )
        org.junit.jupiter.api.Assertions.assertEquals(1, rows!!)
    }

    @Test
    fun `notification is retried after delivery failure`() {
        val customerId = createCustomer(email = "customer@example.com")
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        val mail = mockk<JavaMailSender>()
        every { mail.send(any<SimpleMailMessage>()) } throws org.springframework.mail.MailSendException("unavailable") andThen Unit
        val worker = StatusNotificationWorker(jdbcTemplate, TransactionTemplate(transactionManager), mail, "shop@example.com")
        worker.sendNext()
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", jdbcTemplate.queryForObject(
            "SELECT state FROM status_notifications WHERE service_order_id = ?", String::class.java, java.util.UUID.fromString(orderId),
        ))
        jdbcTemplate.update("UPDATE status_notifications SET next_attempt_at = now() WHERE service_order_id = ?", java.util.UUID.fromString(orderId))
        worker.sendNext()
        org.junit.jupiter.api.Assertions.assertEquals("SENT", jdbcTemplate.queryForObject(
            "SELECT state FROM status_notifications WHERE service_order_id = ?", String::class.java, java.util.UUID.fromString(orderId),
        ))
        verify(exactly = 2) { mail.send(any<SimpleMailMessage>()) }
    }

    @Test
    fun `concurrent notification workers claim an event once`() {
        val customerId = createCustomer(email = "customer@example.com")
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        createServiceOrder(serviceId, partId, vehicleId)
        val mail = mockk<JavaMailSender>(relaxed = true)
        val worker = StatusNotificationWorker(jdbcTemplate, TransactionTemplate(transactionManager), mail, "shop@example.com")
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        val start = java.util.concurrent.CountDownLatch(1)
        try {
            val jobs = (1..2).map { executor.submit { start.await(); worker.sendNext() } }
            start.countDown()
            jobs.forEach { it.get() }
            verify(exactly = 1) { mail.send(any<SimpleMailMessage>()) }
            org.junit.jupiter.api.Assertions.assertEquals("SENT", jdbcTemplate.queryForObject("SELECT state FROM status_notifications", String::class.java))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `expired notification lease stops after maximum attempts`() {
        val customerId = createCustomer(email = "customer@example.com")
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        createServiceOrder(serviceId, partId, vehicleId)
        jdbcTemplate.update("UPDATE status_notifications SET state = 'PROCESSING', attempts = 5, lease_until = now() - interval '1 minute'")
        val mail = mockk<JavaMailSender>(relaxed = true)
        val worker = StatusNotificationWorker(jdbcTemplate, TransactionTemplate(transactionManager), mail, "shop@example.com")
        worker.sendNext()
        verify(exactly = 0) { mail.send(any<SimpleMailMessage>()) }
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", jdbcTemplate.queryForObject("SELECT state FROM status_notifications", String::class.java))
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "PHASE2_PERFORMANCE", matches = "true")
    fun `measure prioritized listing with ten thousand orders`() {
        val customerId = createCustomer()
        val vehicleId = createVehicle(customerId)
        val serviceId = createService()
        val partId = createInventoryItem()
        val (orderId, _) = createServiceOrder(serviceId, partId, vehicleId)
        jdbcTemplate.update(
            """INSERT INTO service_orders (id, customer_id, vehicle_id, customer_document_type, customer_document_masked,
                 customer_name, vehicle_license_plate, vehicle_brand, vehicle_model, vehicle_year, status,
                 tracking_token_hash, tracking_expires_at, tracking_revoked_at, created_at, updated_at, version)
               SELECT gen_random_uuid(), customer_id, vehicle_id, customer_document_type, customer_document_masked,
                 customer_name, vehicle_license_plate, vehicle_brand, vehicle_model, vehicle_year,
                 (ARRAY['RECEIVED','IN_DIAGNOSIS','AWAITING_APPROVAL','IN_EXECUTION','FINISHED','DELIVERED'])[1 + (number % 6)],
                 md5(number::text) || md5(number::text), tracking_expires_at, NULL,
                 created_at - (number * interval '1 second'), updated_at, 0
               FROM service_orders CROSS JOIN generate_series(1, 9999) AS number WHERE id = ?""",
            java.util.UUID.fromString(orderId),
        )
        jdbcTemplate.update(
            """INSERT INTO service_order_items (id, service_order_id, source_type, source_id, description_snapshot,
                 quantity, unit_price_snapshot, consumed_quantity, additional_repair)
               SELECT gen_random_uuid(), orders.id, item.source_type, item.source_id, item.description_snapshot,
                 item.quantity, item.unit_price_snapshot, 0, false
               FROM service_orders orders CROSS JOIN service_order_items item
               WHERE orders.id <> ? AND item.service_order_id = ? AND item.source_type = 'SERVICE'""",
            java.util.UUID.fromString(orderId), java.util.UUID.fromString(orderId),
        )
        fun measure(): Long {
            val started = System.nanoTime()
            mockMvc.get("/api/v1/admin/service-orders/active") { bearer(); param("size", "20") }.andExpect { status { isOk() } }
            return (System.nanoTime() - started) / 1_000_000
        }
        repeat(10) { measure() }
        val samples = (1..50).map { measure() }.sorted()
        val p95 = samples[47]
        println("PHASE2_PERFORMANCE orders=10000 samples=50 pageSize=20 p95Ms=$p95")
        val queryPlan = jdbcTemplate.queryForList(
            """EXPLAIN ANALYZE SELECT id FROM service_orders
               WHERE status IN ('IN_EXECUTION', 'AWAITING_APPROVAL', 'IN_DIAGNOSIS', 'RECEIVED')
               ORDER BY CASE status WHEN 'IN_EXECUTION' THEN 0 WHEN 'AWAITING_APPROVAL' THEN 1
                 WHEN 'IN_DIAGNOSIS' THEN 2 ELSE 3 END, created_at, id LIMIT 20""", String::class.java,
        )
        println(queryPlan.joinToString("\n"))
        org.junit.jupiter.api.Assertions.assertTrue(p95 < 500, "p95 exceeded 500 ms: $p95 ms")
    }

    private fun createCustomer(document: String = "52998224725", email: String? = null): String {
        val body = mockMvc.post("/api/v1/admin/customers") {
            bearer(); json(); content = """{"document":"$document","name":"Maria Silva","email":${email?.let { "\"$it\"" } ?: "null"}}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return extract(body, "id")
    }

    private fun assertTrackingStatus(orderId: String, trackingToken: String, expectedStatus: String) {
        mockMvc.get("/api/v1/tracking/service-orders/$orderId") {
            header("X-Service-Order-Token", trackingToken)
        }.andExpect {
            status { isOk() }; jsonPath("$.status") { value(expectedStatus) }
        }
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
