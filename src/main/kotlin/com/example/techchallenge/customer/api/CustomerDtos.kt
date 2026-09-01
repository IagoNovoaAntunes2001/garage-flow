package com.example.techchallenge.customer.api

import com.example.techchallenge.customer.domain.model.Customer
import com.example.techchallenge.customer.domain.repository.CustomerPage
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CustomerCreateRequest(
    @field:NotBlank val document: String,
    @field:NotBlank @field:Size(min = 2, max = 150) val name: String,
    @field:Email @field:Size(max = 254) val email: String? = null,
    @field:Size(max = 30) val phone: String? = null,
)

data class CustomerUpdateRequest(
    @field:NotBlank @field:Size(min = 2, max = 150) val name: String,
    @field:Email @field:Size(max = 254) val email: String? = null,
    @field:Size(max = 30) val phone: String? = null,
    val active: Boolean = true,
)

data class CustomerResponse(
    val id: UUID,
    val document: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CustomerPageResponse(
    val content: List<CustomerResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun Customer.toResponse() = CustomerResponse(id.value, document.value, name, email, phone, active, createdAt, updatedAt)
fun CustomerPage.toResponse() = CustomerPageResponse(content.map(Customer::toResponse), page, size, totalElements, totalPages)
