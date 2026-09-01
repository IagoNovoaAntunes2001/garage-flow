package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.ServiceOrderAuditLogger
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompleteServiceOrder(
    private val orders: ServiceOrderRepository,
    private val clock: ClockProvider,
    private val audit: ServiceOrderAuditLogger,
) {
    @Transactional
    fun finish(id: ServiceOrderId, actor: String) = mutate(id) { it.finish(clock.now(), actor).also { updated -> audit.lifecycle(id, it.status, updated.status) } }

    @Transactional
    fun deliver(id: ServiceOrderId, actor: String) = mutate(id) { it.deliver(clock.now(), actor).also { updated -> audit.lifecycle(id, it.status, updated.status) } }

    private fun mutate(id: ServiceOrderId, block: (com.example.techchallenge.serviceorder.domain.model.ServiceOrder) -> com.example.techchallenge.serviceorder.domain.model.ServiceOrder) =
        orders.save(block(orders.findByIdForUpdate(id) ?: throw ResourceNotFoundException("ServiceOrder not found")))
}
