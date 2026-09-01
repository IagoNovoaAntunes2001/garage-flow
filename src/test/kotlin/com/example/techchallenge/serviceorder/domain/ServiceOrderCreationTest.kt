package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ServiceOrderCreationTest {
    @Test
    fun `creates service order in received with party and item snapshots`() {
        val order = serviceOrder()

        assertEquals(ServiceOrderStatus.RECEIVED, order.status)
        assertEquals("Maria Silva", order.customerSnapshot.name)
        assertEquals("ABC1D23", order.vehicleSnapshot.licensePlate)
        assertEquals(2, order.items.size)
        assertEquals(ServiceOrderStatus.RECEIVED, order.statusHistory.single().toStatus)
    }

    @Test
    fun `requires at least one service item`() {
        val fixture = serviceOrder()
        assertThrows(IllegalArgumentException::class.java) {
            ServiceOrder.create(
                fixture.id,
                fixture.customerSnapshot,
                fixture.vehicleSnapshot,
                listOf(ServiceOrderItem(UUID.randomUUID(), ItemSourceType.PART, UUID.randomUUID(), "Part", 1, Money.of("10.00"))),
                fixture.trackingTokenHash,
                fixture.trackingExpiresAt,
                Instant.parse("2026-08-31T12:00:00Z"),
            )
        }
    }
}
