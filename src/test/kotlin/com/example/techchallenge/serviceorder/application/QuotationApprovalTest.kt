package com.example.techchallenge.serviceorder.application

import com.example.techchallenge.serviceorder.application.port.CustomerAccessToken
import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import com.example.techchallenge.serviceorder.application.port.ExecutionHistoryEvent
import com.example.techchallenge.serviceorder.application.port.OrderExecutionHistory
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.application.usecase.DecideQuotation
import com.example.techchallenge.serviceorder.domain.model.QuotationState
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

class QuotationApprovalTest {
    @Test
    fun `approves current quotation version through customer token`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val order = serviceOrder(now)
            .startDiagnosis(now.plusSeconds(10), "admin")
            .requestApproval(emptyList(), now.plusSeconds(20), "admin")
        val repository = ApprovalFakeOrderRepository(order)
        val useCase = DecideQuotation(repository, ApprovalTokenPort, ClockProvider { now.plusSeconds(30) }, ServiceOrderAuditLogger())

        val approved = useCase.approve(order.id, "valid-token", 1)

        assertEquals(QuotationState.APPROVED, approved.currentQuotation!!.state)
        assertEquals(QuotationState.APPROVED, repository.saved!!.currentQuotation!!.state)
    }

    @Test
    fun `conceals approval attempts with a wrong customer token`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val order = serviceOrder(now)
            .startDiagnosis(now.plusSeconds(10), "admin")
            .requestApproval(emptyList(), now.plusSeconds(20), "admin")
        val useCase = DecideQuotation(ApprovalFakeOrderRepository(order), ApprovalTokenPort, ClockProvider { now.plusSeconds(30) }, ServiceOrderAuditLogger())

        assertThrows(ResourceNotFoundException::class.java) {
            useCase.reject(order.id, "wrong-token", 1, "Need another estimate")
        }
    }
}

private object ApprovalTokenPort : CustomerAccessTokenPort {
    override fun issueToken(): CustomerAccessToken = CustomerAccessToken("valid-token", "a".repeat(64))
    override fun hash(rawToken: String): String = if (rawToken == "valid-token") "a".repeat(64) else "x".repeat(64)
}

private class ApprovalFakeOrderRepository(private var order: ServiceOrder) : ServiceOrderRepository {
    var saved: ServiceOrder? = null

    override fun save(order: ServiceOrder): ServiceOrder {
        this.order = order
        this.saved = order
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
