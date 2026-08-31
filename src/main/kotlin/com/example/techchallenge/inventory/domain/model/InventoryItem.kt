package com.example.techchallenge.inventory.domain.model

import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.InventoryMovementId
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.ServiceOrderId
import java.time.Instant
import java.util.UUID

enum class InventoryItemType { PART, SUPPLY }
enum class MovementType { STOCK_ADDED, STOCK_REMOVED, ORDER_CONSUMED, ORDER_RETURNED }

data class InventoryMovement(
    val id: InventoryMovementId,
    val inventoryItemId: InventoryItemId,
    val serviceOrderId: ServiceOrderId?,
    val serviceOrderItemId: UUID?,
    val type: MovementType,
    val quantity: Long,
    val resultingQuantity: Long,
    val reason: String?,
    val occurredAt: Instant,
    val actorId: AdministratorId,
)

class InventoryItem private constructor(
    val id: InventoryItemId,
    val type: InventoryItemType,
    val name: String,
    val description: String,
    val unitPrice: Money,
    val availableQuantity: Long,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    fun update(name: String, description: String, unitPrice: Money, active: Boolean, now: Instant): InventoryItem =
        InventoryItem(id, type, cleanName(name), cleanDescription(description), unitPrice, availableQuantity, active, createdAt, now, version)

    fun addStock(quantity: Long, now: Instant): InventoryItem =
        InventoryItem(id, type, name, description, unitPrice, checkedQuantity(availableQuantity + positive(quantity)), active, createdAt, now, version)

    fun removeStock(quantity: Long, now: Instant): InventoryItem {
        val next = availableQuantity - positive(quantity)
        if (next < 0) throw DomainValidationException(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for inventory item")
        return InventoryItem(id, type, name, description, unitPrice, next, active, createdAt, now, version)
    }

    fun deactivate(now: Instant): InventoryItem =
        InventoryItem(id, type, name, description, unitPrice, availableQuantity, false, createdAt, now, version)

    companion object {
        fun create(id: InventoryItemId, type: InventoryItemType, name: String, description: String, unitPrice: Money, quantity: Long, now: Instant): InventoryItem =
            InventoryItem(id, type, cleanName(name), cleanDescription(description), unitPrice, checkedQuantity(quantity), true, now, now, 0)

        fun restore(
            id: InventoryItemId,
            type: InventoryItemType,
            name: String,
            description: String,
            unitPrice: Money,
            availableQuantity: Long,
            active: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
            version: Long,
        ): InventoryItem = InventoryItem(id, type, cleanName(name), cleanDescription(description), unitPrice, checkedQuantity(availableQuantity), active, createdAt, updatedAt, version)

        fun positive(value: Long): Long {
            if (value <= 0) throw DomainValidationException(ErrorCode.INVALID_INVENTORY_ITEM, "Quantity must be positive")
            return value
        }

        private fun checkedQuantity(value: Long): Long {
            if (value < 0) throw DomainValidationException(ErrorCode.INVALID_INVENTORY_ITEM, "Inventory stock cannot be negative")
            return value
        }

        private fun cleanName(value: String): String {
            val cleaned = value.trim()
            if (cleaned.length !in 2..120) invalid("Inventory item name must contain 2 to 120 characters")
            return cleaned
        }

        private fun cleanDescription(value: String): String {
            val cleaned = value.trim()
            if (cleaned.length !in 2..1000) invalid("Inventory item description must contain 2 to 1000 characters")
            return cleaned
        }

        private fun invalid(message: String): Nothing =
            throw DomainValidationException(ErrorCode.INVALID_INVENTORY_ITEM, message)
    }
}
