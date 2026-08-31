package com.example.techchallenge.catalog.domain.repository

import com.example.techchallenge.catalog.domain.model.CatalogService
import com.example.techchallenge.shared.domain.CatalogServiceId

data class CatalogServicePage(
    val content: List<CatalogService>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

interface CatalogServiceRepository {
    fun save(service: CatalogService): CatalogService
    fun findById(id: CatalogServiceId): CatalogService?
    fun existsActiveByName(name: String): Boolean
    fun list(page: Int, size: Int): CatalogServicePage
    fun isReferenced(id: CatalogServiceId): Boolean
    fun delete(service: CatalogService)
}
