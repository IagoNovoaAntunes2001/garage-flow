package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.ServiceOrderAuditLogger
import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DecideQuotation(
    private val orders: ServiceOrderRepository,
    private val tokens: CustomerAccessTokenPort,
    private val clock: ClockProvider,
    private val audit: ServiceOrderAuditLogger,
) {
    @Transactional
    fun approve(id: ServiceOrderId, rawToken: String, quotationVersion: Int): com.example.techchallenge.serviceorder.domain.model.ServiceOrder {
        val order = access(id, rawToken).approveQuotation(quotationVersion, clock.now())
        audit.approval(id, true)
        return orders.save(order)
    }

    @Transactional
    fun reject(id: ServiceOrderId, rawToken: String, quotationVersion: Int, reason: String?): com.example.techchallenge.serviceorder.domain.model.ServiceOrder {
        val order = access(id, rawToken).rejectQuotation(quotationVersion, reason, clock.now())
        audit.approval(id, false)
        return orders.save(order)
    }

    private fun access(id: ServiceOrderId, rawToken: String) =
        orders.findByTrackingHash(id, tokens.hash(rawToken), clock.now()) ?: throw ResourceNotFoundException("ServiceOrder not found")
}
