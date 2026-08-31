package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.QuotationState
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AdditionalRepairTest {
    @Test
    fun `additional repairs supersede quotation and require approval again`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val executing = serviceOrder(now).startDiagnosis(now.plusSeconds(1), "admin")
            .requestApproval(emptyList(), now.plusSeconds(2), "admin")
            .approveQuotation(1, now.plusSeconds(3))
            .startExecution(emptySet(), now.plusSeconds(4), "admin")

        val additional = ServiceOrderItem(UUID.randomUUID(), ItemSourceType.SERVICE, UUID.randomUUID(), "Brake inspection", 1, Money.of("80.00"), additionalRepair = true)
        val waiting = executing.requestApproval(listOf(additional), now.plusSeconds(5), "admin")

        assertEquals(ServiceOrderStatus.AWAITING_APPROVAL, waiting.status)
        assertEquals(QuotationState.SUPERSEDED, waiting.quotations.first().state)
        assertTrue(waiting.items.any { it.additionalRepair })
    }
}
