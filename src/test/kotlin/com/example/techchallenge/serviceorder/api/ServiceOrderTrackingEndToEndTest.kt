package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.serviceorder.domain.serviceOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ServiceOrderTrackingEndToEndTest {
    @Test
    fun `maps service order status to customer-readable tracking progress`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val executing = serviceOrder(now)
            .startDiagnosis(now.plusSeconds(1), "admin")
            .requestApproval(emptyList(), now.plusSeconds(2), "admin")
            .approveQuotation(1, now.plusSeconds(3))
            .startExecution(emptySet(), now.plusSeconds(4), "admin")

        val response = executing.toCustomerTrackingResponse()

        assertEquals(ServiceOrderStatus.IN_EXECUTION, response.status)
        assertEquals("Approved service is in execution", response.progress)
    }
}
