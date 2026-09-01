package com.example.techchallenge.serviceorder.application

import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.serviceOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ExecutionInventoryIntegrationTest {
    @Test
    fun `starting execution marks only inventory lines as consumed and keeps service lines unchanged`() {
        val now = Instant.parse("2026-08-31T12:00:00Z")
        val approved = serviceOrder(now)
            .startDiagnosis(now.plusSeconds(1), "admin")
            .requestApproval(emptyList(), now.plusSeconds(2), "admin")
            .approveQuotation(1, now.plusSeconds(3))

        val inventoryLineIds = approved.items.filter { it.sourceType != ItemSourceType.SERVICE }.map { it.id }.toSet()
        val executing = approved.startExecution(inventoryLineIds, now.plusSeconds(4), "admin")

        assertEquals(0, executing.items.single { it.sourceType == ItemSourceType.SERVICE }.consumedQuantity)
        assertEquals(2, executing.items.single { it.sourceType == ItemSourceType.PART }.consumedQuantity)
    }
}
