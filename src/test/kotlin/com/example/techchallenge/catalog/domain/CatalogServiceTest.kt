package com.example.techchallenge.catalog.domain

import com.example.techchallenge.catalog.domain.model.CatalogService
import com.example.techchallenge.shared.domain.CatalogServiceId
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class CatalogServiceTest {
    private val now = Instant.parse("2026-08-31T12:00:00Z")

    @Test
    fun `creates and updates a priced service`() {
        val service = CatalogService.create(CatalogServiceId.new(), "Oil Change", "Engine oil replacement", Money.of("120.00"), now)
        val updated = service.update("Oil and filter", "Oil and filter replacement", Money.of("180.50"), true, now.plusSeconds(60))

        assertEquals("Oil and filter", updated.name)
        assertEquals("180.50", updated.currentPrice.amount.toPlainString())
    }

    @Test
    fun `rejects invalid service data`() {
        assertThrows(DomainValidationException::class.java) {
            CatalogService.create(CatalogServiceId.new(), "A", "Valid description", Money.of("10.00"), now)
        }
        assertThrows(DomainValidationException::class.java) {
            CatalogService.create(CatalogServiceId.new(), "Valid name", "x", Money.of("10.00"), now)
        }
    }

    @Test
    fun `deactivates a referenced service without changing price snapshot`() {
        val service = CatalogService.create(CatalogServiceId.new(), "Alignment", "Wheel alignment", Money.of("90.00"), now)
        val inactive = service.deactivate(now.plusSeconds(1))

        assertFalse(inactive.active)
        assertEquals("90.00", inactive.currentPrice.amount.toPlainString())
    }
}
