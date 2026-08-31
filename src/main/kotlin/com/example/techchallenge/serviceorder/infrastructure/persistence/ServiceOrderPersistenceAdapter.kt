package com.example.techchallenge.serviceorder.infrastructure.persistence

import com.example.techchallenge.serviceorder.application.port.ExecutionHistoryEvent
import com.example.techchallenge.serviceorder.application.port.OrderExecutionHistory
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.domain.model.ActorType
import com.example.techchallenge.serviceorder.domain.model.Approval
import com.example.techchallenge.serviceorder.domain.model.ApprovalChannel
import com.example.techchallenge.serviceorder.domain.model.ApprovalDecision
import com.example.techchallenge.serviceorder.domain.model.CustomerSnapshot
import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.Quotation
import com.example.techchallenge.serviceorder.domain.model.QuotationLine
import com.example.techchallenge.serviceorder.domain.model.QuotationState
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.serviceorder.domain.model.StatusHistoryEntry
import com.example.techchallenge.serviceorder.domain.model.VehicleSnapshot
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.ApprovalId
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.DocumentType
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.QuotationId
import com.example.techchallenge.shared.domain.ServiceOrderId
import com.example.techchallenge.shared.domain.StatusHistoryId
import com.example.techchallenge.shared.domain.VehicleId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "service_orders")
class ServiceOrderEntity(
    @Id val id: UUID,
    @Column(name = "customer_id", nullable = false) val customerId: UUID,
    @Column(name = "vehicle_id", nullable = false) val vehicleId: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "customer_document_type", nullable = false, length = 4) val customerDocumentType: DocumentType,
    @Column(name = "customer_document_masked", nullable = false, length = 18) val customerDocumentMasked: String,
    @Column(name = "customer_name", nullable = false, length = 150) val customerName: String,
    @Column(name = "vehicle_license_plate", nullable = false, length = 7) val vehicleLicensePlate: String,
    @Column(name = "vehicle_brand", nullable = false, length = 80) val vehicleBrand: String,
    @Column(name = "vehicle_model", nullable = false, length = 100) val vehicleModel: String,
    @Column(name = "vehicle_year", nullable = false) val vehicleYear: Int,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val status: ServiceOrderStatus,
    @Column(name = "tracking_token_hash", nullable = false, length = 64) val trackingTokenHash: String,
    @Column(name = "tracking_expires_at") val trackingExpiresAt: Instant?,
    @Column(name = "tracking_revoked_at") val trackingRevokedAt: Instant?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Version @Column(nullable = false) val version: Long = 0,
)

@Entity
@Table(name = "service_order_items")
class ServiceOrderItemEntity(
    @Id val id: UUID,
    @Column(name = "service_order_id", nullable = false) val serviceOrderId: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 7) val sourceType: ItemSourceType,
    @Column(name = "source_id", nullable = false) val sourceId: UUID,
    @Column(name = "description_snapshot", nullable = false, length = 1000) val descriptionSnapshot: String,
    @Column(nullable = false) val quantity: Long,
    @Column(name = "unit_price_snapshot", nullable = false) val unitPriceSnapshot: BigDecimal,
    @Column(name = "consumed_quantity", nullable = false) val consumedQuantity: Long,
    @Column(name = "additional_repair", nullable = false) val additionalRepair: Boolean,
)

@Entity
@Table(name = "quotations")
class QuotationEntity(
    @Id val id: UUID,
    @Column(name = "service_order_id", nullable = false) val serviceOrderId: UUID,
    @Column(name = "version_number", nullable = false) val versionNumber: Int,
    @Column(name = "service_subtotal", nullable = false) val serviceSubtotal: BigDecimal,
    @Column(name = "inventory_subtotal", nullable = false) val inventorySubtotal: BigDecimal,
    @Column(nullable = false) val total: BigDecimal,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val state: QuotationState,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "requested_at") val requestedAt: Instant?,
)

@Entity
@Table(name = "quotation_lines")
class QuotationLineEntity(
    @Id val id: UUID,
    @Column(name = "quotation_id", nullable = false) val quotationId: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 7) val sourceType: ItemSourceType,
    @Column(name = "source_id", nullable = false) val sourceId: UUID,
    @Column(name = "description_snapshot", nullable = false, length = 1000) val descriptionSnapshot: String,
    @Column(nullable = false) val quantity: Long,
    @Column(name = "unit_price", nullable = false) val unitPrice: BigDecimal,
    @Column(name = "line_total", nullable = false) val lineTotal: BigDecimal,
)

@Entity
@Table(name = "approvals")
class ApprovalEntity(
    @Id val id: UUID,
    @Column(name = "quotation_id", nullable = false) val quotationId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) val decision: ApprovalDecision,
    @Column(name = "decided_at", nullable = false) val decidedAt: Instant,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) val channel: ApprovalChannel,
    @Column(length = 500) val reason: String?,
)

@Entity
@Table(name = "service_order_status_history")
class StatusHistoryEntity(
    @Id val id: UUID,
    @Column(name = "service_order_id", nullable = false) val serviceOrderId: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 20) val fromStatus: ServiceOrderStatus?,
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 20) val toStatus: ServiceOrderStatus,
    @Column(name = "occurred_at", nullable = false) val occurredAt: Instant,
    @Enumerated(EnumType.STRING) @Column(name = "actor_type", nullable = false, length = 13) val actorType: ActorType,
    @Column(name = "actor_reference", length = 100) val actorReference: String?,
    @Column(length = 500) val reason: String?,
)

interface SpringDataServiceOrderRepository : JpaRepository<ServiceOrderEntity, UUID> {
    fun findAllByStatus(status: ServiceOrderStatus, pageable: Pageable): Page<ServiceOrderEntity>
    fun findByIdAndTrackingTokenHashAndTrackingRevokedAtIsNull(id: UUID, trackingTokenHash: String): ServiceOrderEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ServiceOrderEntity o where o.id = :id")
    fun findLocked(@Param("id") id: UUID): ServiceOrderEntity?
}

interface SpringDataServiceOrderItemRepository : JpaRepository<ServiceOrderItemEntity, UUID> {
    fun findAllByServiceOrderId(serviceOrderId: UUID): List<ServiceOrderItemEntity>
}

interface SpringDataQuotationRepository : JpaRepository<QuotationEntity, UUID> {
    fun findAllByServiceOrderId(serviceOrderId: UUID): List<QuotationEntity>
}

interface SpringDataQuotationLineRepository : JpaRepository<QuotationLineEntity, UUID> {
    fun findAllByQuotationId(quotationId: UUID): List<QuotationLineEntity>
}

interface SpringDataApprovalRepository : JpaRepository<ApprovalEntity, UUID> {
    fun findAllByQuotationIdIn(quotationIds: Collection<UUID>): List<ApprovalEntity>
}

interface SpringDataStatusHistoryRepository : JpaRepository<StatusHistoryEntity, UUID> {
    fun findAllByServiceOrderIdOrderByOccurredAtAsc(serviceOrderId: UUID): List<StatusHistoryEntity>
    fun findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByServiceOrderIdAscOccurredAtAsc(from: Instant, to: Instant): List<StatusHistoryEntity>
}

@Repository
class ServiceOrderPersistenceAdapter(
    private val orders: SpringDataServiceOrderRepository,
    private val items: SpringDataServiceOrderItemRepository,
    private val quotations: SpringDataQuotationRepository,
    private val quotationLines: SpringDataQuotationLineRepository,
    private val approvals: SpringDataApprovalRepository,
    private val history: SpringDataStatusHistoryRepository,
) : ServiceOrderRepository {
    override fun save(order: ServiceOrder): ServiceOrder {
        orders.save(order.toEntity())
        items.saveAll(order.items.map { it.toEntity(order.id.value) })
        quotations.saveAll(order.quotations.map { it.toEntity(order.id.value) })
        quotationLines.saveAll(order.quotations.flatMap { quotation -> quotation.lines.map { it.toEntity(quotation.id.value) } })
        approvals.saveAll(order.approvals.map { it.toEntity() })
        history.saveAll(order.statusHistory.map { it.toEntity(order.id.value) })
        return findById(order.id) ?: order
    }

    override fun findById(id: ServiceOrderId): ServiceOrder? = orders.findById(id.value).orElse(null)?.toDomain()
    override fun findByIdForUpdate(id: ServiceOrderId): ServiceOrder? = orders.findLocked(id.value)?.toDomain()

    override fun findByTrackingHash(id: ServiceOrderId, trackingTokenHash: String, now: Instant): ServiceOrder? =
        orders.findByIdAndTrackingTokenHashAndTrackingRevokedAtIsNull(id.value, trackingTokenHash)
            ?.takeIf { it.trackingExpiresAt == null || it.trackingExpiresAt.isAfter(now) }
            ?.toDomain()

    override fun list(status: ServiceOrderStatus?, page: Int, size: Int): PageResponse<ServiceOrder> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = if (status == null) orders.findAll(pageable) else orders.findAllByStatus(status, pageable)
        return PageResponse(result.content.map { it.toDomain() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    override fun executionHistory(from: Instant, to: Instant): List<OrderExecutionHistory> {
        val rows = history.findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByServiceOrderIdAscOccurredAtAsc(from, to)
        return rows.groupBy { it.serviceOrderId }
            .map { (id, events) -> OrderExecutionHistory(ServiceOrderId(id), events.map { ExecutionHistoryEvent(it.toStatus, it.occurredAt) }) }
    }

    private fun ServiceOrderEntity.toDomain(): ServiceOrder {
        val orderItems = items.findAllByServiceOrderId(id).map { it.toDomain() }
        val orderQuotations = quotations.findAllByServiceOrderId(id).sortedBy { it.versionNumber }.map { quotation ->
            quotation.toDomain(quotationLines.findAllByQuotationId(quotation.id))
        }
        val orderApprovals = approvals.findAllByQuotationIdIn(orderQuotations.map { it.id.value }).map { it.toDomain() }
        val orderHistory = history.findAllByServiceOrderIdOrderByOccurredAtAsc(id).map { it.toDomain() }
        return ServiceOrder.restore(
            ServiceOrderId(id),
            CustomerSnapshot(CustomerId(customerId), customerDocumentType, customerDocumentMasked, customerName),
            VehicleSnapshot(VehicleId(vehicleId), vehicleLicensePlate, vehicleBrand, vehicleModel, vehicleYear),
            status,
            orderItems,
            orderQuotations,
            orderApprovals,
            orderHistory,
            trackingTokenHash,
            trackingExpiresAt,
            trackingRevokedAt,
            createdAt,
            updatedAt,
            version,
        )
    }

    private fun ServiceOrder.toEntity() = ServiceOrderEntity(
        id.value, customerId.value, vehicleId.value, customerSnapshot.documentType, customerSnapshot.maskedDocument, customerSnapshot.name,
        vehicleSnapshot.licensePlate, vehicleSnapshot.brand, vehicleSnapshot.model, vehicleSnapshot.year, status,
        trackingTokenHash, trackingExpiresAt, trackingRevokedAt, createdAt, updatedAt, version,
    )

    private fun ServiceOrderItem.toEntity(orderId: UUID) = ServiceOrderItemEntity(id, orderId, sourceType, sourceId, descriptionSnapshot, quantity, unitPriceSnapshot.amount, consumedQuantity, additionalRepair)
    private fun ServiceOrderItemEntity.toDomain() = ServiceOrderItem(id, sourceType, sourceId, descriptionSnapshot, quantity, Money.of(unitPriceSnapshot), consumedQuantity, additionalRepair)

    private fun Quotation.toEntity(orderId: UUID) = QuotationEntity(id.value, orderId, versionNumber, serviceSubtotal.amount, inventorySubtotal.amount, total.amount, state, createdAt, requestedAt)
    private fun QuotationEntity.toDomain(lines: List<QuotationLineEntity>) = Quotation(QuotationId(id), versionNumber, lines.map { it.toDomain() }, state, createdAt, requestedAt)

    private fun QuotationLine.toEntity(quotationId: UUID) = QuotationLineEntity(id, quotationId, sourceType, sourceId, descriptionSnapshot, quantity, unitPrice.amount, lineTotal.amount)
    private fun QuotationLineEntity.toDomain() = QuotationLine(id, sourceType, sourceId, descriptionSnapshot, quantity, Money.of(unitPrice))

    private fun Approval.toEntity() = ApprovalEntity(id.value, quotationId.value, decision, decidedAt, channel, reason)
    private fun ApprovalEntity.toDomain() = Approval(ApprovalId(id), QuotationId(quotationId), decision, decidedAt, channel, reason)

    private fun StatusHistoryEntry.toEntity(orderId: UUID) = StatusHistoryEntity(id.value, orderId, fromStatus, toStatus, occurredAt, actorType, actorReference, reason)
    private fun StatusHistoryEntity.toDomain() = StatusHistoryEntry(StatusHistoryId(id), fromStatus, toStatus, occurredAt, actorType, actorReference, reason)
}
