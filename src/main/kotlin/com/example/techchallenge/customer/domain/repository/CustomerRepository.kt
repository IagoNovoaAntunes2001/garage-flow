package com.example.techchallenge.customer.domain.repository

import com.example.techchallenge.customer.domain.model.Customer
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.Document

data class CustomerPage(
    val content: List<Customer>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

interface CustomerRepository {
    fun save(customer: Customer): Customer
    fun findById(id: CustomerId): Customer?
    fun findByDocument(document: Document): Customer?
    fun existsByDocument(document: Document): Boolean
    fun list(document: Document?, page: Int, size: Int): CustomerPage
    fun isReferenced(id: CustomerId): Boolean
    fun delete(customer: Customer)
}
