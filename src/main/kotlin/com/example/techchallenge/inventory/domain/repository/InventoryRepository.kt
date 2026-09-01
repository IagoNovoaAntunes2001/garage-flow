package com.example.techchallenge.inventory.domain.repository

import com.example.techchallenge.inventory.domain.model.InventoryItem
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.inventory.domain.model.InventoryMovement
import com.example.techchallenge.shared.domain.InventoryItemId

data class InventoryItemPage(
    val content: List<InventoryItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

interface InventoryRepository {
    fun save(item: InventoryItem): InventoryItem
    fun findById(id: InventoryItemId): InventoryItem?
    fun findActiveByIds(ids: Collection<InventoryItemId>): List<InventoryItem>
    fun lockByIdsInOrder(ids: Collection<InventoryItemId>): List<InventoryItem>
    fun existsActiveByName(name: String): Boolean
    fun list(type: InventoryItemType?, page: Int, size: Int): InventoryItemPage
    fun appendMovement(movement: InventoryMovement)
    fun isReferenced(id: InventoryItemId): Boolean
    fun delete(item: InventoryItem)
}
