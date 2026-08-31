package com.example.techchallenge.shared.domain

import java.util.UUID

interface DomainId {
    val value: UUID
}

@JvmInline value class CustomerId(override val value: UUID) : DomainId { companion object { fun new() = CustomerId(UUID.randomUUID()) } }
@JvmInline value class VehicleId(override val value: UUID) : DomainId { companion object { fun new() = VehicleId(UUID.randomUUID()) } }
@JvmInline value class CatalogServiceId(override val value: UUID) : DomainId { companion object { fun new() = CatalogServiceId(UUID.randomUUID()) } }
@JvmInline value class InventoryItemId(override val value: UUID) : DomainId { companion object { fun new() = InventoryItemId(UUID.randomUUID()) } }
@JvmInline value class ServiceOrderId(override val value: UUID) : DomainId { companion object { fun new() = ServiceOrderId(UUID.randomUUID()) } }
@JvmInline value class QuotationId(override val value: UUID) : DomainId { companion object { fun new() = QuotationId(UUID.randomUUID()) } }
@JvmInline value class ApprovalId(override val value: UUID) : DomainId { companion object { fun new() = ApprovalId(UUID.randomUUID()) } }
@JvmInline value class StatusHistoryId(override val value: UUID) : DomainId { companion object { fun new() = StatusHistoryId(UUID.randomUUID()) } }
@JvmInline value class InventoryMovementId(override val value: UUID) : DomainId { companion object { fun new() = InventoryMovementId(UUID.randomUUID()) } }
@JvmInline value class AdministratorId(override val value: UUID) : DomainId { companion object { fun new() = AdministratorId(UUID.randomUUID()) } }
