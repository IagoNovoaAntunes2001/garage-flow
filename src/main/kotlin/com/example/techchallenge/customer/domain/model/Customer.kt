package com.example.techchallenge.customer.domain.model

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.Document
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import java.time.Instant

class Customer private constructor(
    val id: CustomerId,
    val document: Document,
    val name: String,
    val email: String?,
    val phone: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    fun update(name: String, email: String?, phone: String?, active: Boolean, now: Instant): Customer =
        Customer(id, document, validName(name), validEmail(email), validPhone(phone), active, createdAt, now, version)

    fun deactivate(now: Instant): Customer =
        Customer(id, document, name, email, phone, false, createdAt, now, version)

    companion object {
        fun create(
            id: CustomerId,
            document: Document,
            name: String,
            email: String?,
            phone: String?,
            now: Instant,
        ) = Customer(id, document, validName(name), validEmail(email), validPhone(phone), true, now, now, 0)

        fun restore(
            id: CustomerId,
            document: Document,
            name: String,
            email: String?,
            phone: String?,
            active: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
            version: Long,
        ) = Customer(id, document, name, email, phone, active, createdAt, updatedAt, version)

        private fun validName(value: String): String = value.trim().also {
            if (it.length !in 2..150) invalid("Customer name must contain between 2 and 150 characters")
        }

        private fun validEmail(value: String?): String? = value?.trim()?.lowercase()?.also {
            if (it.length > 254 || !EMAIL.matches(it)) invalid("Customer email is invalid")
        }

        private fun validPhone(value: String?): String? = value?.trim()?.also {
            if (it.isBlank() || it.length > 30) invalid("Customer phone must contain between 1 and 30 characters")
        }

        private fun invalid(message: String): Nothing =
            throw DomainValidationException(ErrorCode.INVALID_CUSTOMER, message)

        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
