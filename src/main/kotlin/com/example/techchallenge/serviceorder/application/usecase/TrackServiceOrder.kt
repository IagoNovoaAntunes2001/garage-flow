package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.port.CustomerAccessTokenPort
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrackServiceOrder(
    private val orders: ServiceOrderRepository,
    private val tokens: CustomerAccessTokenPort,
    private val clock: ClockProvider,
) {
    @Transactional(readOnly = true)
    fun execute(id: ServiceOrderId, rawToken: String) =
        orders.findByTrackingHash(id, tokens.hash(rawToken), clock.now()) ?: throw ResourceNotFoundException("ServiceOrder not found")
}
