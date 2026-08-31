package com.example.techchallenge.serviceorder.domain.model

import com.example.techchallenge.shared.domain.ServiceOrderId
import com.example.techchallenge.shared.domain.StatusHistoryId
import java.time.Instant

class ServiceOrder private constructor(
    val id: ServiceOrderId,
    val customerSnapshot: CustomerSnapshot,
    val vehicleSnapshot: VehicleSnapshot,
    val status: ServiceOrderStatus,
    val items: List<ServiceOrderItem>,
    val quotations: List<Quotation>,
    val approvals: List<Approval>,
    val statusHistory: List<StatusHistoryEntry>,
    val trackingTokenHash: String,
    val trackingExpiresAt: Instant?,
    val trackingRevokedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    val customerId = customerSnapshot.customerId
    val vehicleId = vehicleSnapshot.vehicleId
    val currentQuotation: Quotation? = quotations.maxByOrNull { it.versionNumber }

    fun startDiagnosis(now: Instant, actor: String): ServiceOrder {
        ServiceOrderLifecycle.ensure(status, ServiceOrderStatus.RECEIVED, "start diagnosis")
        return transition(ServiceOrderStatus.IN_DIAGNOSIS, now, actor)
    }

    fun requestApproval(extraItems: List<ServiceOrderItem>, now: Instant, actor: String): ServiceOrder {
        if (status != ServiceOrderStatus.IN_DIAGNOSIS && status != ServiceOrderStatus.IN_EXECUTION) {
            ServiceOrderLifecycle.invalid("Cannot request approval when ServiceOrder is $status")
        }
        val nextItems = items + extraItems
        val nextVersion = (quotations.maxOfOrNull { it.versionNumber } ?: 0) + 1
        val newQuotation = Quotation.fromItems(nextVersion, nextItems, now).requestApproval(now)
        val nextQuotations = quotations.map {
            if (it.state == QuotationState.AWAITING_APPROVAL || it.state == QuotationState.APPROVED || it.state == QuotationState.DRAFT) it.supersede() else it
        } + newQuotation
        return copy(
            status = ServiceOrderStatus.AWAITING_APPROVAL,
            items = nextItems,
            quotations = nextQuotations,
            updatedAt = now,
            statusHistory = statusHistory + history(status, ServiceOrderStatus.AWAITING_APPROVAL, now, ActorType.ADMINISTRATOR, actor, "Quotation approval requested"),
        )
    }

    fun approveQuotation(versionNumber: Int, now: Instant): ServiceOrder {
        val quotation = currentPendingQuotation(versionNumber)
        return copy(
            quotations = quotations.map { if (it.id == quotation.id) it.approve() else it },
            approvals = approvals + Approval.approve(quotation.id, now),
            updatedAt = now,
        )
    }

    fun rejectQuotation(versionNumber: Int, reason: String?, now: Instant): ServiceOrder {
        val quotation = currentPendingQuotation(versionNumber)
        return copy(
            quotations = quotations.map { if (it.id == quotation.id) it.reject() else it },
            approvals = approvals + Approval.reject(quotation.id, reason, now),
            updatedAt = now,
        )
    }

    fun startExecution(consumedItems: Set<java.util.UUID>, now: Instant, actor: String): ServiceOrder {
        ServiceOrderLifecycle.ensure(status, ServiceOrderStatus.AWAITING_APPROVAL, "start execution")
        if (currentQuotation?.state != QuotationState.APPROVED) ServiceOrderLifecycle.approvalRequired()
        return copy(
            status = ServiceOrderStatus.IN_EXECUTION,
            items = items.map { if (it.id in consumedItems) it.consumeOutstanding() else it },
            updatedAt = now,
            statusHistory = statusHistory + history(status, ServiceOrderStatus.IN_EXECUTION, now, ActorType.ADMINISTRATOR, actor, "Execution started"),
        )
    }

    fun finish(now: Instant, actor: String): ServiceOrder {
        ServiceOrderLifecycle.ensure(status, ServiceOrderStatus.IN_EXECUTION, "finish service order")
        return transition(ServiceOrderStatus.FINISHED, now, actor)
    }

    fun deliver(now: Instant, actor: String): ServiceOrder {
        ServiceOrderLifecycle.ensure(status, ServiceOrderStatus.FINISHED, "deliver vehicle")
        return transition(ServiceOrderStatus.DELIVERED, now, actor)
    }

    private fun currentPendingQuotation(versionNumber: Int): Quotation {
        val quotation = currentQuotation ?: ServiceOrderLifecycle.staleApproval()
        if (quotation.versionNumber != versionNumber) ServiceOrderLifecycle.staleApproval()
        if (quotation.state != QuotationState.AWAITING_APPROVAL) ServiceOrderLifecycle.staleApproval()
        return quotation
    }

    private fun transition(next: ServiceOrderStatus, now: Instant, actor: String): ServiceOrder =
        copy(status = next, updatedAt = now, statusHistory = statusHistory + history(status, next, now, ActorType.ADMINISTRATOR, actor, null))

    private fun copy(
        status: ServiceOrderStatus = this.status,
        items: List<ServiceOrderItem> = this.items,
        quotations: List<Quotation> = this.quotations,
        approvals: List<Approval> = this.approvals,
        statusHistory: List<StatusHistoryEntry> = this.statusHistory,
        trackingRevokedAt: Instant? = this.trackingRevokedAt,
        updatedAt: Instant = this.updatedAt,
    ) = ServiceOrder(id, customerSnapshot, vehicleSnapshot, status, items, quotations, approvals, statusHistory, trackingTokenHash, trackingExpiresAt, trackingRevokedAt, createdAt, updatedAt, version)

    companion object {
        fun create(
            id: ServiceOrderId,
            customerSnapshot: CustomerSnapshot,
            vehicleSnapshot: VehicleSnapshot,
            items: List<ServiceOrderItem>,
            trackingTokenHash: String,
            trackingExpiresAt: Instant?,
            now: Instant,
        ): ServiceOrder {
            require(items.any { it.sourceType == ItemSourceType.SERVICE }) { "ServiceOrder requires at least one service" }
            val initialHistory = history(null, ServiceOrderStatus.RECEIVED, now, ActorType.ADMINISTRATOR, null, "Service order created")
            return ServiceOrder(id, customerSnapshot, vehicleSnapshot, ServiceOrderStatus.RECEIVED, items, emptyList(), emptyList(), listOf(initialHistory), trackingTokenHash, trackingExpiresAt, null, now, now, 0)
        }

        fun restore(
            id: ServiceOrderId,
            customerSnapshot: CustomerSnapshot,
            vehicleSnapshot: VehicleSnapshot,
            status: ServiceOrderStatus,
            items: List<ServiceOrderItem>,
            quotations: List<Quotation>,
            approvals: List<Approval>,
            statusHistory: List<StatusHistoryEntry>,
            trackingTokenHash: String,
            trackingExpiresAt: Instant?,
            trackingRevokedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
            version: Long,
        ): ServiceOrder = ServiceOrder(id, customerSnapshot, vehicleSnapshot, status, items, quotations, approvals, statusHistory, trackingTokenHash, trackingExpiresAt, trackingRevokedAt, createdAt, updatedAt, version)

        private fun history(
            from: ServiceOrderStatus?,
            to: ServiceOrderStatus,
            now: Instant,
            actorType: ActorType,
            actor: String?,
            reason: String?,
        ) = StatusHistoryEntry(StatusHistoryId.new(), from, to, now, actorType, actor, reason)
    }
}
