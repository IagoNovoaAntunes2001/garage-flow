package com.example.techchallenge.serviceorder.domain.model

import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.QuotationId
import java.time.Instant
import java.util.UUID

enum class QuotationState { DRAFT, AWAITING_APPROVAL, APPROVED, REJECTED, SUPERSEDED }

data class QuotationLine(
    val id: UUID,
    val sourceType: ItemSourceType,
    val sourceId: UUID,
    val descriptionSnapshot: String,
    val quantity: Long,
    val unitPrice: Money,
) {
    val lineTotal: Money = unitPrice.multiply(quantity)
}

data class Quotation(
    val id: QuotationId,
    val versionNumber: Int,
    val lines: List<QuotationLine>,
    val state: QuotationState,
    val createdAt: Instant,
    val requestedAt: Instant?,
) {
    init {
        require(versionNumber > 0) { "Quotation version must be positive" }
        require(lines.any { it.sourceType == ItemSourceType.SERVICE }) { "Quotation must contain at least one service" }
    }

    val serviceSubtotal: Money = lines.filter { it.sourceType == ItemSourceType.SERVICE }.fold(Money.ZERO) { subtotal, line -> subtotal + line.lineTotal }
    val inventorySubtotal: Money = lines.filter { it.sourceType != ItemSourceType.SERVICE }.fold(Money.ZERO) { subtotal, line -> subtotal + line.lineTotal }
    val total: Money = serviceSubtotal + inventorySubtotal

    fun requestApproval(now: Instant): Quotation = copy(state = QuotationState.AWAITING_APPROVAL, requestedAt = now)
    fun approve(): Quotation = copy(state = QuotationState.APPROVED)
    fun reject(): Quotation = copy(state = QuotationState.REJECTED)
    fun supersede(): Quotation = copy(state = QuotationState.SUPERSEDED)

    companion object {
        fun fromItems(versionNumber: Int, items: List<ServiceOrderItem>, now: Instant): Quotation =
            Quotation(
                id = QuotationId.new(),
                versionNumber = versionNumber,
                lines = items.map {
                    QuotationLine(UUID.randomUUID(), it.sourceType, it.sourceId, it.descriptionSnapshot, it.quantity, it.unitPriceSnapshot)
                },
                state = QuotationState.DRAFT,
                createdAt = now,
                requestedAt = null,
            )
    }
}
