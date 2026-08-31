package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryServiceOrders(private val orders: ServiceOrderRepository) {
    @Transactional(readOnly = true)
    fun get(id: ServiceOrderId) = orders.findById(id) ?: throw ResourceNotFoundException("ServiceOrder not found")

    @Transactional(readOnly = true)
    fun list(status: ServiceOrderStatus?, page: PageRequestDto) = orders.list(status, page.page, page.size)
}
