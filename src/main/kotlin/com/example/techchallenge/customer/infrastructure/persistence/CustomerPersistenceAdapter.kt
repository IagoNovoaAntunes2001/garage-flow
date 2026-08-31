package com.example.techchallenge.customer.infrastructure.persistence

import com.example.techchallenge.customer.domain.model.Customer
import com.example.techchallenge.customer.domain.repository.CustomerPage
import com.example.techchallenge.customer.domain.repository.CustomerRepository
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.Document
import com.example.techchallenge.shared.domain.DocumentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customers")
class CustomerEntity(
    @Id val id: UUID,
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 4)
    val documentType: DocumentType,
    @Column(name = "document_value", nullable = false, unique = true, length = 14)
    val documentValue: String,
    @Column(nullable = false, length = 150) val name: String,
    @Column(length = 254) val email: String?,
    @Column(length = 30) val phone: String?,
    @Column(nullable = false) val active: Boolean,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Version @Column(nullable = false) val version: Long = 0,
)

interface SpringDataCustomerRepository : JpaRepository<CustomerEntity, UUID> {
    fun findByDocumentValue(documentValue: String): CustomerEntity?
    fun existsByDocumentValue(documentValue: String): Boolean
    fun findAllByDocumentValue(documentValue: String, pageable: Pageable): Page<CustomerEntity>
}

@Repository
class CustomerPersistenceAdapter(
    private val repository: SpringDataCustomerRepository,
    private val entityManager: EntityManager,
) : CustomerRepository {
    override fun save(customer: Customer): Customer = repository.save(customer.toEntity()).toDomain()
    override fun findById(id: CustomerId): Customer? = repository.findById(id.value).orElse(null)?.toDomain()
    override fun findByDocument(document: Document): Customer? = repository.findByDocumentValue(document.value)?.toDomain()
    override fun existsByDocument(document: Document): Boolean = repository.existsByDocumentValue(document.value)

    override fun list(document: Document?, page: Int, size: Int): CustomerPage {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name", "id"))
        val result = if (document == null) repository.findAll(pageable) else repository.findAllByDocumentValue(document.value, pageable)
        return CustomerPage(result.content.map { it.toDomain() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    override fun isReferenced(id: CustomerId): Boolean {
        val count = entityManager.createNativeQuery(
            "SELECT (SELECT count(*) FROM vehicles WHERE customer_id = :id) + (SELECT count(*) FROM service_orders WHERE customer_id = :id)",
        ).setParameter("id", id.value).singleResult as Number
        return count.toLong() > 0
    }

    override fun delete(customer: Customer) = repository.deleteById(customer.id.value)

    private fun CustomerEntity.toDomain() = Customer.restore(
        CustomerId(id), Document.from(documentValue), name, email, phone, active, createdAt, updatedAt, version,
    )

    private fun Customer.toEntity() = CustomerEntity(
        id.value, document.type, document.value, name, email, phone, active, createdAt, updatedAt, version,
    )
}
