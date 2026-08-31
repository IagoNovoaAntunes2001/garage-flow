package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.inventory.domain.model.InventoryMovement
import com.example.techchallenge.inventory.domain.model.MovementType
import com.example.techchallenge.inventory.domain.repository.InventoryRepository
import com.example.techchallenge.serviceorder.application.ServiceOrderAuditLogger
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.QuotationState
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderLifecycle
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.InventoryMovementId
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StartExecution(
    private val orders: ServiceOrderRepository,
    private val inventory: InventoryRepository,
    private val clock: ClockProvider,
    private val audit: ServiceOrderAuditLogger,
) {
    @Transactional
    fun execute(id: ServiceOrderId, actorId: AdministratorId): com.example.techchallenge.serviceorder.domain.model.ServiceOrder {
        val order = orders.findByIdForUpdate(id) ?: throw ResourceNotFoundException("ServiceOrder not found")
        if (order.status != ServiceOrderStatus.AWAITING_APPROVAL) {
            ServiceOrderLifecycle.invalid("Cannot start execution when ServiceOrder is ${order.status}")
        }
        if (order.currentQuotation?.state != QuotationState.APPROVED) {
            ServiceOrderLifecycle.approvalRequired()
        }
        val stockLines = order.items.filter { it.sourceType != ItemSourceType.SERVICE && it.outstandingQuantity > 0 }
        val locked = inventory.lockByIdsInOrder(stockLines.map { InventoryItemId(it.sourceId) }).associateBy { it.id.value }
        val now = clock.now()
        stockLines.forEach { line ->
            val item = locked[line.sourceId] ?: throw ResourceNotFoundException("Inventory item not found")
            val updated = inventory.save(item.removeStock(line.outstandingQuantity, now))
            inventory.appendMovement(
                InventoryMovement(
                    InventoryMovementId.new(),
                    updated.id,
                    id,
                    line.id,
                    MovementType.ORDER_CONSUMED,
                    line.outstandingQuantity,
                    updated.availableQuantity,
                    "ServiceOrder execution",
                    now,
                    actorId,
                ),
            )
        }
        val updatedOrder = order.startExecution(stockLines.map { it.id }.toSet(), now, actorId.value.toString())
        audit.inventoryConsumed(id, stockLines.size)
        audit.lifecycle(id, order.status, updatedOrder.status)
        return orders.save(updatedOrder)
    }
}
