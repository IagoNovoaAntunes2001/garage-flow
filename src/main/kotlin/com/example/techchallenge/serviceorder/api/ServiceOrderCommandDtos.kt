package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.serviceorder.application.usecase.RepairItemCommand
import com.example.techchallenge.serviceorder.domain.model.Approval
import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.Quotation
import com.example.techchallenge.serviceorder.domain.model.QuotationLine
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.serviceorder.domain.model.StatusHistoryEntry
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RepairItemRequest(
    val type: ItemSourceType,
    val referenceId: UUID,
    @field:Min(1) val quantity: Long,
) {
    fun toCommand() = RepairItemCommand(type, referenceId, quantity)
}

data class RepairItemsRequest(@field:Valid val items: List<RepairItemRequest> = emptyList())

data class CreateServiceOrderRequest(
    @field:NotBlank val customerDocument: String,
    val vehicleId: UUID,
    @field:Valid val items: List<RepairItemRequest>,
)

data class ApprovalRequest(
    @field:Min(1) val quotationVersion: Int,
    @field:Size(max = 500) val reason: String? = null,
)

data class CreateServiceOrderResponse(val serviceOrder: ServiceOrderDetailResponse, val trackingToken: String)
data class OrderActionResponse(val serviceOrder: ServiceOrderDetailResponse)

data class ServiceOrderDetailResponse(
    val id: UUID,
    val customer: CustomerSnapshotResponse,
    val vehicle: VehicleSnapshotResponse,
    val status: ServiceOrderStatus,
    val items: List<ServiceOrderItemResponse>,
    val currentQuotation: QuotationResponse?,
    val quotations: List<QuotationResponse>,
    val approvals: List<ApprovalResponse>,
    val statusHistory: List<StatusHistoryResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CustomerSnapshotResponse(val id: UUID, val documentType: String, val maskedDocument: String, val name: String)
data class VehicleSnapshotResponse(val id: UUID, val licensePlate: String, val brand: String, val model: String, val year: Int)
data class ServiceOrderItemResponse(val id: UUID, val type: ItemSourceType, val sourceId: UUID, val description: String, val quantity: Long, val unitPrice: String, val consumedQuantity: Long, val additionalRepair: Boolean)
data class QuotationResponse(val id: UUID, val versionNumber: Int, val state: String, val serviceSubtotal: String, val inventorySubtotal: String, val total: String, val lines: List<QuotationLineResponse>, val createdAt: Instant, val requestedAt: Instant?)
data class QuotationLineResponse(val id: UUID, val type: ItemSourceType, val sourceId: UUID, val description: String, val quantity: Long, val unitPrice: String, val lineTotal: String)
data class ApprovalResponse(val id: UUID, val quotationId: UUID, val decision: String, val decidedAt: Instant, val reason: String?)
data class StatusHistoryResponse(val fromStatus: ServiceOrderStatus?, val toStatus: ServiceOrderStatus, val occurredAt: Instant, val reason: String?)

fun ServiceOrder.toDetailResponse() = ServiceOrderDetailResponse(
    id.value,
    CustomerSnapshotResponse(customerId.value, customerSnapshot.documentType.name, customerSnapshot.maskedDocument, customerSnapshot.name),
    VehicleSnapshotResponse(vehicleId.value, vehicleSnapshot.licensePlate, vehicleSnapshot.brand, vehicleSnapshot.model, vehicleSnapshot.year),
    status,
    items.map { it.toResponse() },
    currentQuotation?.toResponse(),
    quotations.map { it.toResponse() },
    approvals.map { it.toResponse() },
    statusHistory.map { it.toResponse() },
    createdAt,
    updatedAt,
)

fun ServiceOrderItem.toResponse() = ServiceOrderItemResponse(id, sourceType, sourceId, descriptionSnapshot, quantity, unitPriceSnapshot.amount.toPlainString(), consumedQuantity, additionalRepair)
fun Quotation.toResponse() = QuotationResponse(id.value, versionNumber, state.name, serviceSubtotal.amount.toPlainString(), inventorySubtotal.amount.toPlainString(), total.amount.toPlainString(), lines.map { it.toResponse() }, createdAt, requestedAt)
fun QuotationLine.toResponse() = QuotationLineResponse(id, sourceType, sourceId, descriptionSnapshot, quantity, unitPrice.amount.toPlainString(), lineTotal.amount.toPlainString())
fun Approval.toResponse() = ApprovalResponse(id.value, quotationId.value, decision.name, decidedAt, reason)
fun StatusHistoryEntry.toResponse() = StatusHistoryResponse(fromStatus, toStatus, occurredAt, reason)
