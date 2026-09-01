package com.example.techchallenge.inventory.infrastructure.persistence

import com.example.techchallenge.inventory.domain.model.InventoryItem
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.inventory.domain.model.InventoryMovement
import com.example.techchallenge.inventory.domain.model.MovementType
import com.example.techchallenge.inventory.domain.repository.InventoryItemPage
import com.example.techchallenge.inventory.domain.repository.InventoryRepository
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.InventoryItemId
import com.example.techchallenge.shared.domain.InventoryMovementId
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.ServiceOrderId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_items")
class InventoryItemEntity(
    @Id val id: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 6) val type: InventoryItemType,
    @Column(nullable = false, length = 120) val name: String,
    @Column(nullable = false, length = 1000) val description: String,
    @Column(name = "unit_price", nullable = false) val unitPrice: BigDecimal,
    @Column(name = "available_quantity", nullable = false) val availableQuantity: Long,
    @Column(nullable = false) val active: Boolean,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Version @Column(nullable = false) val version: Long = 0,
)

@Entity
@Table(name = "inventory_movements")
class InventoryMovementEntity(
    @Id val id: UUID,
    @Column(name = "inventory_item_id", nullable = false) val inventoryItemId: UUID,
    @Column(name = "service_order_id") val serviceOrderId: UUID?,
    @Column(name = "service_order_item_id") val serviceOrderItemId: UUID?,
    @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 20) val type: MovementType,
    @Column(nullable = false) val quantity: Long,
    @Column(name = "resulting_quantity", nullable = false) val resultingQuantity: Long,
    @Column(length = 500) val reason: String?,
    @Column(name = "occurred_at", nullable = false) val occurredAt: Instant,
    @Column(name = "actor_id", nullable = false) val actorId: UUID,
)

interface SpringDataInventoryItemRepository : JpaRepository<InventoryItemEntity, UUID> {
    fun existsByNameIgnoreCaseAndActiveTrue(name: String): Boolean
    fun findAllByType(type: InventoryItemType, pageable: Pageable): Page<InventoryItemEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItemEntity i where i.id in :ids order by i.id")
    fun lockAllByIdIn(@Param("ids") ids: Collection<UUID>): List<InventoryItemEntity>

    fun findAllByIdInAndActiveTrue(ids: Collection<UUID>): List<InventoryItemEntity>
}

interface SpringDataInventoryMovementRepository : JpaRepository<InventoryMovementEntity, UUID>

@Repository
class InventoryPersistenceAdapter(
    private val items: SpringDataInventoryItemRepository,
    private val movements: SpringDataInventoryMovementRepository,
    private val entityManager: EntityManager,
) : InventoryRepository {
    override fun save(item: InventoryItem): InventoryItem = items.save(item.toEntity()).toDomain()
    override fun findById(id: InventoryItemId): InventoryItem? = items.findById(id.value).orElse(null)?.toDomain()
    override fun findActiveByIds(ids: Collection<InventoryItemId>): List<InventoryItem> =
        if (ids.isEmpty()) emptyList() else items.findAllByIdInAndActiveTrue(ids.map { it.value }).map { it.toDomain() }

    override fun lockByIdsInOrder(ids: Collection<InventoryItemId>): List<InventoryItem> =
        if (ids.isEmpty()) emptyList() else items.lockAllByIdIn(ids.map { it.value }.sorted()).map { it.toDomain() }

    override fun existsActiveByName(name: String): Boolean = items.existsByNameIgnoreCaseAndActiveTrue(name.trim())

    override fun list(type: InventoryItemType?, page: Int, size: Int): InventoryItemPage {
        val pageable = PageRequest.of(page, size, Sort.by("name", "id"))
        val result = if (type == null) items.findAll(pageable) else items.findAllByType(type, pageable)
        return InventoryItemPage(result.content.map { it.toDomain() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    override fun appendMovement(movement: InventoryMovement) = movements.save(movement.toEntity()).let { }

    override fun isReferenced(id: InventoryItemId): Boolean {
        val count = entityManager.createNativeQuery(
            "SELECT (SELECT count(*) FROM service_order_items WHERE source_type IN ('PART','SUPPLY') AND source_id = :id) + " +
                "(SELECT count(*) FROM inventory_movements WHERE inventory_item_id = :id)",
        )
            .setParameter("id", id.value)
            .singleResult as Number
        return count.toLong() > 0
    }

    override fun delete(item: InventoryItem) = items.deleteById(item.id.value)

    private fun InventoryItemEntity.toDomain() = InventoryItem.restore(
        InventoryItemId(id), type, name, description, Money.of(unitPrice), availableQuantity, active, createdAt, updatedAt, version,
    )

    private fun InventoryItem.toEntity() = InventoryItemEntity(
        id.value, type, name, description, unitPrice.amount, availableQuantity, active, createdAt, updatedAt, version,
    )

    private fun InventoryMovement.toEntity() = InventoryMovementEntity(
        id.value, inventoryItemId.value, serviceOrderId?.value, serviceOrderItemId, type, quantity, resultingQuantity, reason, occurredAt, actorId.value,
    )
}
