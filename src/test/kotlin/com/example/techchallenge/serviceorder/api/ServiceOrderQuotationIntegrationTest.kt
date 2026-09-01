package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.serviceorder.domain.serviceOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ServiceOrderQuotationIntegrationTest {
    @Test
    fun `preserves quotation values as a snapshot response`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val order = serviceOrder(now)
            .startDiagnosis(now.plusSeconds(1), "admin")
            .requestApproval(emptyList(), now.plusSeconds(2), "admin")

        val quotation = order.toDetailResponse().currentQuotation!!

        assertEquals("AWAITING_APPROVAL", quotation.state)
        assertEquals("120.00", quotation.serviceSubtotal)
        assertEquals("70.00", quotation.inventorySubtotal)
        assertEquals("190.00", quotation.total)
    }
}
