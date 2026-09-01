package com.example.techchallenge.serviceorder.domain.model

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.DocumentType
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.StatusHistoryId
import com.example.techchallenge.shared.domain.VehicleId
import java.time.Instant
import java.util.UUID

enum class ServiceOrderStatus { RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, IN_EXECUTION, FINISHED, DELIVERED }
enum class ItemSourceType { SERVICE, PART, SUPPLY }
enum class ActorType { ADMINISTRATOR, CUSTOMER }

data class CustomerSnapshot(
    val customerId: CustomerId,
    val documentType: DocumentType,
    val maskedDocument: String,
    val name: String,
)

data class VehicleSnapshot(
    val vehicleId: VehicleId,
    val licensePlate: String,
    val brand: String,
    val model: String,
    val year: Int,
)

data class ServiceOrderItem(
    val id: UUID,
    val sourceType: ItemSourceType,
    val sourceId: UUID,
    val descriptionSnapshot: String,
    val quantity: Long,
    val unitPriceSnapshot: Money,
    val consumedQuantity: Long = 0,
    val additionalRepair: Boolean = false,
) {
    init {
        require(quantity > 0) { "ServiceOrderItem quantity must be positive" }
        require(consumedQuantity in 0..quantity) { "Consumed quantity must be between zero and requested quantity" }
    }

    val lineTotal: Money = unitPriceSnapshot.multiply(quantity)
    val outstandingQuantity: Long = quantity - consumedQuantity

    fun consumeOutstanding(): ServiceOrderItem = copy(consumedQuantity = quantity)
}

data class StatusHistoryEntry(
    val id: StatusHistoryId,
    val fromStatus: ServiceOrderStatus?,
    val toStatus: ServiceOrderStatus,
    val occurredAt: Instant,
    val actorType: ActorType,
    val actorReference: String?,
    val reason: String?,
)
