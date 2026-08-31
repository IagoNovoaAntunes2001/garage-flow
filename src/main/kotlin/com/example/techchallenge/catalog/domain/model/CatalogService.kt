package com.example.techchallenge.catalog.domain.model

import com.example.techchallenge.shared.domain.CatalogServiceId
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import com.example.techchallenge.shared.domain.Money
import java.time.Instant

class CatalogService private constructor(
    val id: CatalogServiceId,
    val name: String,
    val description: String,
    val currentPrice: Money,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    fun update(name: String, description: String, currentPrice: Money, active: Boolean, now: Instant): CatalogService =
        CatalogService(id, cleanName(name), cleanDescription(description), currentPrice, active, createdAt, now, version)

    fun deactivate(now: Instant): CatalogService =
        CatalogService(id, name, description, currentPrice, false, createdAt, now, version)

    companion object {
        fun create(id: CatalogServiceId, name: String, description: String, price: Money, now: Instant): CatalogService =
            CatalogService(id, cleanName(name), cleanDescription(description), price, true, now, now, 0)

        fun restore(
            id: CatalogServiceId,
            name: String,
            description: String,
            price: Money,
            active: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
            version: Long,
        ): CatalogService = CatalogService(id, cleanName(name), cleanDescription(description), price, active, createdAt, updatedAt, version)

        private fun cleanName(value: String): String {
            val cleaned = value.trim()
            if (cleaned.length !in 2..120) invalid("Catalog service name must contain 2 to 120 characters")
            return cleaned
        }

        private fun cleanDescription(value: String): String {
            val cleaned = value.trim()
            if (cleaned.length !in 2..1000) invalid("Catalog service description must contain 2 to 1000 characters")
            return cleaned
        }

        private fun invalid(message: String): Nothing =
            throw DomainValidationException(ErrorCode.INVALID_CATALOG_SERVICE, message)
    }
}
