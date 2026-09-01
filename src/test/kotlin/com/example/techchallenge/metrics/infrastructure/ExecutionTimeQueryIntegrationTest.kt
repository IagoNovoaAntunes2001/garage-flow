package com.example.techchallenge.metrics.infrastructure

import com.example.techchallenge.metrics.application.ExecutionTimeCalculator
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.support.PostgreSqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class ExecutionTimeQueryIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    @Autowired private lateinit var serviceOrders: ServiceOrderRepository

    @BeforeEach
    fun cleanup() {
        listOf("service_order_status_history", "service_orders", "vehicles", "customers").forEach {
            jdbcTemplate.update("DELETE FROM $it")
        }
    }

    @Test
    fun `loads full history for orders finished inside the metric window`() {
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val startedBeforeWindow = Instant.parse("2026-08-30T23:30:00Z")
        val finishedInsideWindow = Instant.parse("2026-08-31T00:30:00Z")

        jdbcTemplate.update(
            """
            INSERT INTO customers (id, document_type, document_value, name, created_at, updated_at)
            VALUES (?, 'CPF', '52998224725', 'Maria Silva', ?, ?)
            """.trimIndent(),
            customerId,
            Timestamp.from(startedBeforeWindow),
            Timestamp.from(startedBeforeWindow),
        )
        jdbcTemplate.update(
            """
            INSERT INTO vehicles (id, customer_id, license_plate, brand, model, production_year, created_at, updated_at)
            VALUES (?, ?, 'ABC1D23', 'Ford', 'Ka', 2020, ?, ?)
            """.trimIndent(),
            vehicleId,
            customerId,
            Timestamp.from(startedBeforeWindow),
            Timestamp.from(startedBeforeWindow),
        )
        jdbcTemplate.update(
            """
            INSERT INTO service_orders (
                id, customer_id, vehicle_id, customer_document_type, customer_document_masked, customer_name,
                vehicle_license_plate, vehicle_brand, vehicle_model, vehicle_year, status,
                tracking_token_hash, created_at, updated_at
            )
            VALUES (?, ?, ?, 'CPF', '***.982.247-**', 'Maria Silva', 'ABC1D23', 'Ford', 'Ka', 2020, 'FINISHED', ?, ?, ?)
            """.trimIndent(),
            orderId,
            customerId,
            vehicleId,
            "b".repeat(64),
            Timestamp.from(startedBeforeWindow),
            Timestamp.from(finishedInsideWindow),
        )
        insertHistory(orderId, null, ServiceOrderStatus.RECEIVED, startedBeforeWindow.minusSeconds(120))
        insertHistory(orderId, ServiceOrderStatus.RECEIVED, ServiceOrderStatus.IN_DIAGNOSIS, startedBeforeWindow.minusSeconds(60))
        insertHistory(orderId, ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.IN_EXECUTION, startedBeforeWindow)
        insertHistory(orderId, ServiceOrderStatus.IN_EXECUTION, ServiceOrderStatus.FINISHED, finishedInsideWindow)

        val histories = serviceOrders.executionHistory(
            Instant.parse("2026-08-31T00:00:00Z"),
            Instant.parse("2026-09-01T00:00:00Z"),
        )

        assertEquals(1, histories.size)
        assertEquals(3600.0, ExecutionTimeCalculator().averageExecutionSeconds(histories))
    }

    private fun insertHistory(orderId: UUID, from: ServiceOrderStatus?, to: ServiceOrderStatus, occurredAt: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO service_order_status_history (
                id, service_order_id, from_status, to_status, occurred_at, actor_type, actor_reference
            )
            VALUES (?, ?, ?, ?, ?, 'ADMINISTRATOR', 'metric-test')
            """.trimIndent(),
            UUID.randomUUID(),
            orderId,
            from?.name,
            to.name,
            Timestamp.from(occurredAt),
        )
    }
}
