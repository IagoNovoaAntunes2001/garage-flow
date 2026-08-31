package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddAdditionalRepairs(
    private val orders: ServiceOrderRepository,
    private val createServiceOrder: CreateServiceOrder,
    private val clock: ClockProvider,
) {
    @Transactional
    fun execute(id: ServiceOrderId, items: List<RepairItemCommand>, actor: String) =
        orders.save(
            (orders.findByIdForUpdate(id) ?: throw ResourceNotFoundException("ServiceOrder not found"))
                .requestApproval(createServiceOrder.resolveItems(items, additionalRepair = true), clock.now(), actor),
        )
}
