package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class QuotationTest {
    @Test
    fun `calculates exact service inventory and total subtotals from snapshots`() {
        val order = serviceOrder().startDiagnosis(Instant.parse("2026-08-31T12:01:00Z"), "admin")
            .requestApproval(emptyList(), Instant.parse("2026-08-31T12:02:00Z"), "admin")
        val quotation = order.currentQuotation!!

        assertEquals(ServiceOrderStatus.AWAITING_APPROVAL, order.status)
        assertEquals("120.00", quotation.serviceSubtotal.amount.toPlainString())
        assertEquals("70.00", quotation.inventorySubtotal.amount.toPlainString())
        assertEquals("190.00", quotation.total.amount.toPlainString())
    }
}
