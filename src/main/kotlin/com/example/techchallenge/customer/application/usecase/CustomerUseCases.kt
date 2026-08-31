package com.example.techchallenge.customer.application.usecase

import com.example.techchallenge.customer.domain.model.Customer
import com.example.techchallenge.customer.domain.repository.CustomerPage
import com.example.techchallenge.customer.domain.repository.CustomerRepository
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.Document
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerUseCases(
    private val repository: CustomerRepository,
    private val clockProvider: ClockProvider,
) {
    @Transactional
    fun create(documentValue: String, name: String, email: String?, phone: String?): Customer {
        val document = Document.from(documentValue)
        if (repository.existsByDocument(document)) throw ConflictException("A customer with this document already exists")
        return repository.save(Customer.create(CustomerId.new(), document, name, email, phone, clockProvider.now()))
    }

    @Transactional(readOnly = true)
    fun get(id: CustomerId): Customer = repository.findById(id) ?: notFound()

    @Transactional(readOnly = true)
    fun findByDocument(documentValue: String): Customer = repository.findByDocument(Document.from(documentValue)) ?: notFound()

    @Transactional(readOnly = true)
    fun list(documentValue: String?, pagination: PageRequestDto): CustomerPage =
        repository.list(documentValue?.let(Document::from), pagination.page, pagination.size)

    @Transactional
    fun update(id: CustomerId, name: String, email: String?, phone: String?, active: Boolean): Customer {
        val current = repository.findById(id) ?: notFound()
        return repository.save(current.update(name, email, phone, active, clockProvider.now()))
    }

    @Transactional
    fun remove(id: CustomerId) {
        val current = repository.findById(id) ?: notFound()
        if (repository.isReferenced(id)) repository.save(current.deactivate(clockProvider.now())) else repository.delete(current)
    }

    private fun notFound(): Nothing = throw ResourceNotFoundException("Customer not found")
}
