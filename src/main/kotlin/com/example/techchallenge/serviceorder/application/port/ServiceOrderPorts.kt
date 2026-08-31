package com.example.techchallenge.serviceorder.application.port

import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.ServiceOrderId
import java.time.Instant

interface ServiceOrderRepository {
    fun save(order: ServiceOrder): ServiceOrder
    fun findById(id: ServiceOrderId): ServiceOrder?
    fun findByIdForUpdate(id: ServiceOrderId): ServiceOrder?
    fun findByTrackingHash(id: ServiceOrderId, trackingTokenHash: String, now: Instant): ServiceOrder?
    fun list(status: ServiceOrderStatus?, page: Int, size: Int): PageResponse<ServiceOrder>
    fun executionHistory(from: Instant, to: Instant): List<OrderExecutionHistory>
}

data class OrderExecutionHistory(
    val serviceOrderId: ServiceOrderId,
    val events: List<ExecutionHistoryEvent>,
)

data class ExecutionHistoryEvent(
    val status: ServiceOrderStatus,
    val occurredAt: Instant,
)

interface CustomerAccessTokenPort {
    fun issueToken(): CustomerAccessToken
    fun hash(rawToken: String): String
}

data class CustomerAccessToken(
    val rawToken: String,
    val hash: String,
)
