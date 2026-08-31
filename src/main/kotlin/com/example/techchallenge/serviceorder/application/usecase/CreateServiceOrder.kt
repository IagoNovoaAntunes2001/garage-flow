package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.catalog.domain.repository.CatalogServiceRepository
import com.example.techchallenge.customer.domain.repository.CustomerRepository
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.inventory.domain.repository.InventoryRepository
import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.domain.model.CustomerSnapshot
import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.VehicleSnapshot
import com.example.techchallenge.shared.domain.BusinessRuleException
import com.example.techchallenge.shared.domain.CatalogServiceId
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.Document
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.vehicle.domain.repository.VehicleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

data class RepairItemCommand(val type: ItemSourceType, val referenceId: UUID, val quantity: Long)
data class CreatedServiceOrder(val order: ServiceOrder, val trackingToken: String)

@Service
class CreateServiceOrder(
    private val customers: CustomerRepository,
    private val vehicles: VehicleRepository,
    private val catalog: CatalogServiceRepository,
    private val inventory: InventoryRepository,
    private val orders: ServiceOrderRepository,
    private val tokens: CustomerAccessTokenPort,
    private val clock: ClockProvider,
) {
    @Transactional
    fun execute(customerDocument: String, vehicleId: VehicleId, requestedItems: List<RepairItemCommand>): CreatedServiceOrder {
        val customer = customers.findByDocument(Document.from(customerDocument)) ?: throw ResourceNotFoundException("Customer not found")
        if (!customer.active) throw BusinessRuleException("Inactive customer cannot create a ServiceOrder")
        val vehicle = vehicles.findById(vehicleId) ?: throw ResourceNotFoundException("Vehicle not found")
        if (!vehicle.active || vehicle.customerId != customer.id) throw BusinessRuleException("Vehicle must belong to the active customer")
        val items = resolveItems(requestedItems, additionalRepair = false)
        if (items.none { it.sourceType == ItemSourceType.SERVICE }) throw BusinessRuleException("ServiceOrder requires at least one service")
        val issued = tokens.issueToken()
        val now = clock.now()
        val order = ServiceOrder.create(
            ServiceOrderId.new(),
            CustomerSnapshot(customer.id, customer.document.type, customer.document.masked(), customer.name),
            VehicleSnapshot(vehicle.id, vehicle.licensePlate.value, vehicle.brand, vehicle.model, vehicle.year.value),
            items,
            issued.hash,
            now.plus(Duration.ofDays(30)),
            now,
        )
        return CreatedServiceOrder(orders.save(order), issued.rawToken)
    }

    fun resolveItems(commands: List<RepairItemCommand>, additionalRepair: Boolean): List<ServiceOrderItem> =
        commands.map { command ->
            if (command.quantity <= 0) throw BusinessRuleException("Repair item quantity must be positive")
            when (command.type) {
                ItemSourceType.SERVICE -> {
                    val service = catalog.findById(CatalogServiceId(command.referenceId)) ?: throw ResourceNotFoundException("Catalog service not found")
                    if (!service.active) throw BusinessRuleException("Inactive service cannot be selected")
                    ServiceOrderItem(UUID.randomUUID(), ItemSourceType.SERVICE, service.id.value, service.name, command.quantity, service.currentPrice, 0, additionalRepair)
                }
                ItemSourceType.PART, ItemSourceType.SUPPLY -> {
                    val item = inventory.findById(InventoryItemId(command.referenceId)) ?: throw ResourceNotFoundException("Inventory item not found")
                    if (!item.active) throw BusinessRuleException("Inactive inventory item cannot be selected")
                    if ((command.type == ItemSourceType.PART && item.type != InventoryItemType.PART) || (command.type == ItemSourceType.SUPPLY && item.type != InventoryItemType.SUPPLY)) {
                        throw BusinessRuleException("Inventory item type does not match repair item type")
                    }
                    ServiceOrderItem(UUID.randomUUID(), command.type, item.id.value, item.name, command.quantity, item.unitPrice, 0, additionalRepair)
                }
            }
        }
}
