package com.example.techchallenge.inventory.domain

import com.example.techchallenge.inventory.domain.model.InventoryItem
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class InventoryItemTest {
    private val now = Instant.parse("2026-08-31T12:00:00Z")

    @Test
    fun `creates adjusts and updates inventory item`() {
        val item = InventoryItem.create(InventoryItemId.new(), InventoryItemType.PART, "Oil filter", "Engine oil filter", Money.of("35.00"), 4, now)
        val stocked = item.addStock(6, now.plusSeconds(1))
        val consumed = stocked.removeStock(3, now.plusSeconds(2))
        val updated = consumed.update("Premium oil filter", "Premium engine oil filter", Money.of("40.00"), true, now.plusSeconds(3))

        assertEquals(10, stocked.availableQuantity)
        assertEquals(7, consumed.availableQuantity)
        assertEquals("Premium oil filter", updated.name)
    }

    @Test
    fun `rejects negative and insufficient stock`() {
        assertThrows(DomainValidationException::class.java) {
            InventoryItem.create(InventoryItemId.new(), InventoryItemType.SUPPLY, "Brake fluid", "DOT 4 brake fluid", Money.of("55.00"), -1, now)
        }
        val item = InventoryItem.create(InventoryItemId.new(), InventoryItemType.SUPPLY, "Brake fluid", "DOT 4 brake fluid", Money.of("55.00"), 1, now)
        assertThrows(DomainValidationException::class.java) { item.removeStock(2, now.plusSeconds(1)) }
        assertThrows(DomainValidationException::class.java) { item.addStock(0, now.plusSeconds(1)) }
    }

    @Test
    fun `deactivates item preserving quantity`() {
        val item = InventoryItem.create(InventoryItemId.new(), InventoryItemType.PART, "Spark plug", "Ignition spark plug", Money.of("25.00"), 8, now)
        val inactive = item.deactivate(now.plusSeconds(1))

        assertFalse(inactive.active)
        assertEquals(8, inactive.availableQuantity)
    }
}
