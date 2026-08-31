package com.example.techchallenge.inventory.application.usecase

import com.example.techchallenge.inventory.domain.model.InventoryItem
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.inventory.domain.model.InventoryMovement
import com.example.techchallenge.inventory.domain.model.MovementType
import com.example.techchallenge.inventory.domain.repository.InventoryItemPage
import com.example.techchallenge.inventory.domain.repository.InventoryRepository
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.InventoryMovementId
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class InventoryUseCases(
    private val repository: InventoryRepository,
    private val clock: ClockProvider,
) {
    @Transactional
    fun create(type: InventoryItemType, name: String, description: String, unitPrice: BigDecimal, quantity: Long): InventoryItem {
        if (repository.existsActiveByName(name)) duplicate()
        return repository.save(InventoryItem.create(InventoryItemId.new(), type, name, description, Money.of(unitPrice), quantity, clock.now()))
    }

    @Transactional(readOnly = true) fun get(id: InventoryItemId): InventoryItem = repository.findById(id) ?: notFound()
    @Transactional(readOnly = true) fun list(type: InventoryItemType?, page: PageRequestDto): InventoryItemPage = repository.list(type, page.page, page.size)

    @Transactional
    fun update(id: InventoryItemId, name: String, description: String, unitPrice: BigDecimal, active: Boolean): InventoryItem {
        val current = repository.findById(id) ?: notFound()
        if (!name.equals(current.name, ignoreCase = true) && repository.existsActiveByName(name)) duplicate()
        return repository.save(current.update(name, description, Money.of(unitPrice), active, clock.now()))
    }

    @Transactional
    fun adjustStock(id: InventoryItemId, quantity: Long, operation: StockOperation, reason: String, actorId: AdministratorId): InventoryItem {
        val current = repository.findById(id) ?: notFound()
        val now = clock.now()
        val updated = when (operation) {
            StockOperation.ADD -> current.addStock(quantity, now)
            StockOperation.REMOVE -> current.removeStock(quantity, now)
        }
        val saved = repository.save(updated)
        repository.appendMovement(
            InventoryMovement(
                id = InventoryMovementId.new(),
                inventoryItemId = id,
                serviceOrderId = null,
                serviceOrderItemId = null,
                type = if (operation == StockOperation.ADD) MovementType.STOCK_ADDED else MovementType.STOCK_REMOVED,
                quantity = quantity,
                resultingQuantity = saved.availableQuantity,
                reason = reason.trim(),
                occurredAt = now,
                actorId = actorId,
            ),
        )
        return saved
    }

    @Transactional
    fun remove(id: InventoryItemId) {
        val current = repository.findById(id) ?: notFound()
        if (repository.isReferenced(id)) repository.save(current.deactivate(clock.now())) else repository.delete(current)
    }

    private fun duplicate(): Nothing = throw ConflictException("An active inventory item with this name already exists")
    private fun notFound(): Nothing = throw ResourceNotFoundException("Inventory item not found")
}

enum class StockOperation { ADD, REMOVE }
