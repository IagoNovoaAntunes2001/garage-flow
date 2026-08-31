package com.example.techchallenge.shared.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun `creates BRL money with two decimal places and HALF_EVEN rounding`() {
        assertThat(Money.of("10.125").amount).isEqualByComparingTo("10.12")
        assertThat(Money.of("10.135").amount).isEqualByComparingTo("10.14")
        assertThat(Money.of("10").currency).isEqualTo("BRL")
    }

    @Test
    fun `adds values and multiplies by a positive quantity exactly`() {
        val result = Money.of("10.10") + Money.of("2.35")

        assertThat(result).isEqualTo(Money.of("12.45"))
        assertThat(Money.of("7.25").multiply(3)).isEqualTo(Money.of("21.75"))
    }

    @Test
    fun `rejects negative amounts and non-positive quantities`() {
        assertThatThrownBy { Money.of(BigDecimal("-0.01")) }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("negative")
        assertThatThrownBy { Money.of("1.00").multiply(0) }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("positive")
        assertThatThrownBy { Money.of("not-money") }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("amount")
    }
}
