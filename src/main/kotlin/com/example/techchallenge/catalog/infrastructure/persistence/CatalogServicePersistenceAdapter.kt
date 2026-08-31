package com.example.techchallenge.catalog.infrastructure.persistence

import com.example.techchallenge.catalog.domain.model.CatalogService
import com.example.techchallenge.catalog.domain.repository.CatalogServicePage
import com.example.techchallenge.catalog.domain.repository.CatalogServiceRepository
import com.example.techchallenge.shared.domain.CatalogServiceId
import com.example.techchallenge.shared.domain.Money
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalog_services")
class CatalogServiceEntity(
    @Id val id: UUID,
    @Column(nullable = false, length = 120) val name: String,
    @Column(nullable = false, length = 1000) val description: String,
    @Column(name = "current_price", nullable = false) val currentPrice: BigDecimal,
    @Column(nullable = false) val active: Boolean,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Version @Column(nullable = false) val version: Long = 0,
)

interface SpringDataCatalogServiceRepository : JpaRepository<CatalogServiceEntity, UUID> {
    fun existsByNameIgnoreCaseAndActiveTrue(name: String): Boolean
}

@Repository
class CatalogServicePersistenceAdapter(
    private val repository: SpringDataCatalogServiceRepository,
    private val entityManager: EntityManager,
) : CatalogServiceRepository {
    override fun save(service: CatalogService): CatalogService = repository.save(service.toEntity()).toDomain()
    override fun findById(id: CatalogServiceId): CatalogService? = repository.findById(id.value).orElse(null)?.toDomain()
    override fun existsActiveByName(name: String): Boolean = repository.existsByNameIgnoreCaseAndActiveTrue(name.trim())

    override fun list(page: Int, size: Int): CatalogServicePage {
        val result = repository.findAll(PageRequest.of(page, size, Sort.by("name", "id")))
        return CatalogServicePage(result.content.map { it.toDomain() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    override fun isReferenced(id: CatalogServiceId): Boolean {
        val count = entityManager.createNativeQuery("SELECT count(*) FROM service_order_items WHERE source_type = 'SERVICE' AND source_id = :id")
            .setParameter("id", id.value)
            .singleResult as Number
        return count.toLong() > 0
    }

    override fun delete(service: CatalogService) = repository.deleteById(service.id.value)

    private fun CatalogServiceEntity.toDomain() = CatalogService.restore(
        CatalogServiceId(id), name, description, Money.of(currentPrice), active, createdAt, updatedAt, version,
    )

    private fun CatalogService.toEntity() = CatalogServiceEntity(
        id.value, name, description, currentPrice.amount, active, createdAt, updatedAt, version,
    )
}
