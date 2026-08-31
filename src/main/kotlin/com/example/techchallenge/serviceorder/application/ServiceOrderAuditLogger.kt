package com.example.techchallenge.serviceorder.application

import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ServiceOrderAuditLogger {
    fun lifecycle(orderId: ServiceOrderId, from: ServiceOrderStatus, to: ServiceOrderStatus) {
        logger.info("ServiceOrder lifecycle transition orderId={} from={} to={}", orderId.value, from, to)
    }

    fun approval(orderId: ServiceOrderId, approved: Boolean) {
        logger.info("ServiceOrder quotation decision orderId={} approved={}", orderId.value, approved)
    }

    fun inventoryConsumed(orderId: ServiceOrderId, lineCount: Int) {
        logger.info("ServiceOrder inventory consumed orderId={} lineCount={}", orderId.value, lineCount)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceOrderAuditLogger::class.java)
    }
}
