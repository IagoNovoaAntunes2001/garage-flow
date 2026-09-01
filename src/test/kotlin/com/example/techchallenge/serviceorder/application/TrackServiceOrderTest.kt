package com.example.techchallenge.serviceorder.application

import com.example.techchallenge.serviceorder.application.port.CustomerAccessToken
import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import com.example.techchallenge.serviceorder.application.port.ExecutionHistoryEvent
import com.example.techchallenge.serviceorder.application.port.OrderExecutionHistory
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.application.usecase.TrackServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.serviceorder.domain.serviceOrder
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class TrackServiceOrderTest {
    @Test
    fun `returns order only when token hash matches the requested order`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val order = serviceOrder(now)
        val useCase = TrackServiceOrder(FakeOrderRepository(order), FixedTokenPort, ClockProvider { now })

        assertEquals(order.id, useCase.execute(order.id, "valid-token").id)
        assertThrows(ResourceNotFoundException::class.java) {
            useCase.execute(order.id, "wrong-token")
        }
    }
}

private object FixedTokenPort : CustomerAccessTokenPort {
    override fun issueToken(): CustomerAccessToken = CustomerAccessToken("valid-token", "a".repeat(64))
    override fun hash(rawToken: String): String = if (rawToken == "valid-token") "a".repeat(64) else "x".repeat(64)
}

private class FakeOrderRepository(private var order: ServiceOrder) : ServiceOrderRepository {
    override fun save(order: ServiceOrder): ServiceOrder {
        this.order = order
        return order
    }

    override fun findById(id: ServiceOrderId): ServiceOrder? = order.takeIf { it.id == id }
    override fun findByIdForUpdate(id: ServiceOrderId): ServiceOrder? = findById(id)
    override fun findByTrackingHash(id: ServiceOrderId, trackingTokenHash: String, now: Instant): ServiceOrder? =
        order.takeIf { it.id == id && it.trackingTokenHash == trackingTokenHash && it.trackingRevokedAt == null && it.trackingExpiresAt!!.isAfter(now) }

    override fun list(status: ServiceOrderStatus?, page: Int, size: Int): PageResponse<ServiceOrder> =
        PageResponse(listOf(order), page, size, 1, 1)

    override fun executionHistory(from: Instant, to: Instant): List<OrderExecutionHistory> =
        listOf(OrderExecutionHistory(order.id, order.statusHistory.map { ExecutionHistoryEvent(it.toStatus, it.occurredAt) }))
}
