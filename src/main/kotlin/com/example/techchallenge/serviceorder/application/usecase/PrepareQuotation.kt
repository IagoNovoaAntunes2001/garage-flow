package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.ServiceOrderAuditLogger
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PrepareQuotation(
    private val orders: ServiceOrderRepository,
    private val createServiceOrder: CreateServiceOrder,
    private val clock: ClockProvider,
    private val audit: ServiceOrderAuditLogger,
) {
    @Transactional
    fun startDiagnosis(id: ServiceOrderId, actor: String) =
        mutate(id) { it.startDiagnosis(clock.now(), actor).also { updated -> audit.lifecycle(id, it.status, updated.status) } }

    @Transactional
    fun requestApproval(id: ServiceOrderId, extraItems: List<RepairItemCommand>, actor: String) =
        mutate(id) { it.requestApproval(createServiceOrder.resolveItems(extraItems, additionalRepair = false), clock.now(), actor) }

    private fun mutate(id: ServiceOrderId, block: (com.example.techchallenge.serviceorder.domain.model.ServiceOrder) -> com.example.techchallenge.serviceorder.domain.model.ServiceOrder) =
        orders.save(block(orders.findByIdForUpdate(id) ?: throw ResourceNotFoundException("ServiceOrder not found")))
}
