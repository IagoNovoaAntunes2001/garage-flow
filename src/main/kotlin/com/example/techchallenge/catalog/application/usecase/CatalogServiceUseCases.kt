package com.example.techchallenge.catalog.application.usecase

import com.example.techchallenge.catalog.domain.model.CatalogService
import com.example.techchallenge.catalog.domain.repository.CatalogServicePage
import com.example.techchallenge.catalog.domain.repository.CatalogServiceRepository
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.CatalogServiceId
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class CatalogServiceUseCases(
    private val repository: CatalogServiceRepository,
    private val clock: ClockProvider,
) {
    @Transactional
    fun create(name: String, description: String, price: BigDecimal): CatalogService {
        if (repository.existsActiveByName(name)) duplicate()
        return repository.save(CatalogService.create(CatalogServiceId.new(), name, description, Money.of(price), clock.now()))
    }

    @Transactional(readOnly = true) fun get(id: CatalogServiceId): CatalogService = repository.findById(id) ?: notFound()
    @Transactional(readOnly = true) fun list(page: PageRequestDto): CatalogServicePage = repository.list(page.page, page.size)

    @Transactional
    fun update(id: CatalogServiceId, name: String, description: String, price: BigDecimal, active: Boolean): CatalogService {
        val current = repository.findById(id) ?: notFound()
        if (!name.equals(current.name, ignoreCase = true) && repository.existsActiveByName(name)) duplicate()
        return repository.save(current.update(name, description, Money.of(price), active, clock.now()))
    }

    @Transactional
    fun remove(id: CatalogServiceId) {
        val current = repository.findById(id) ?: notFound()
        if (repository.isReferenced(id)) repository.save(current.deactivate(clock.now())) else repository.delete(current)
    }

    private fun duplicate(): Nothing = throw ConflictException("A catalog service with this name already exists")
    private fun notFound(): Nothing = throw ResourceNotFoundException("Catalog service not found")
}
