package com.example.techchallenge.customer.domain

import com.example.techchallenge.customer.domain.model.Customer
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.Document
import com.example.techchallenge.shared.domain.DomainValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class CustomerTest {
    private val createdAt = Instant.parse("2026-08-31T12:00:00Z")

    @Test
    fun `creates customer with normalized values and active state`() {
        val customer = Customer.create(
            CustomerId.new(), Document.from("529.982.247-25"), "  Maria Silva  ",
            " MARIA@EXAMPLE.COM ", " 11 99999-0000 ", createdAt,
        )

        assertThat(customer.name).isEqualTo("Maria Silva")
        assertThat(customer.email).isEqualTo("maria@example.com")
        assertThat(customer.phone).isEqualTo("11 99999-0000")
        assertThat(customer.active).isTrue()
    }

    @Test
    fun `updates mutable contact data but preserves identity and creation time`() {
        val customer = validCustomer()
        val updatedAt = createdAt.plusSeconds(60)

        val updated = customer.update("Maria Souza", null, "11988887777", true, updatedAt)

        assertThat(updated.id).isEqualTo(customer.id)
        assertThat(updated.document).isEqualTo(customer.document)
        assertThat(updated.createdAt).isEqualTo(createdAt)
        assertThat(updated.updatedAt).isEqualTo(updatedAt)
        assertThat(updated.name).isEqualTo("Maria Souza")
    }

    @Test
    fun `deactivates customer without destroying its identity`() {
        val customer = validCustomer()
        val deactivated = customer.deactivate(createdAt.plusSeconds(30))

        assertThat(deactivated.active).isFalse()
        assertThat(deactivated.id).isEqualTo(customer.id)
        assertThat(deactivated.document).isEqualTo(customer.document)
    }

    @Test
    fun `rejects invalid names emails and blank phones`() {
        assertThatThrownBy { validCustomer().update("x", null, null, true, createdAt) }
            .isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { validCustomer().update("Maria Silva", "invalid", null, true, createdAt) }
            .isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { validCustomer().update("Maria Silva", null, "   ", true, createdAt) }
            .isInstanceOf(DomainValidationException::class.java)
    }

    private fun validCustomer() = Customer.create(
        CustomerId.new(), Document.from("52998224725"), "Maria Silva", null, null, createdAt,
    )
}
