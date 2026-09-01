package com.example.techchallenge.shared.domain

import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(
    val amount: BigDecimal,
) {
    val currency: String = BRL

    operator fun plus(other: Money): Money = of(amount.add(other.amount))

    fun multiply(quantity: Long): Money {
        if (quantity <= 0) {
            throw DomainValidationException(
                ErrorCode.INVALID_MONEY,
                "Money quantity must be positive",
            )
        }
        return of(amount.multiply(BigDecimal.valueOf(quantity)))
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Money && amount.compareTo(other.amount) == 0)

    override fun hashCode(): Int = amount.stripTrailingZeros().hashCode()

    override fun toString(): String = "$currency $amount"

    companion object {
        const val BRL = "BRL"
        val ZERO: Money = Money(BigDecimal.ZERO.setScale(2))

        fun of(amount: String): Money = try {
            of(amount.toBigDecimal())
        } catch (exception: NumberFormatException) {
            throw DomainValidationException(
                ErrorCode.INVALID_MONEY,
                "Invalid money amount",
            )
        }

        fun of(amount: BigDecimal): Money {
            if (amount.signum() < 0) {
                throw DomainValidationException(
                    ErrorCode.INVALID_MONEY,
                    "Money amount cannot be negative",
                )
            }
            return Money(amount.setScale(2, RoundingMode.HALF_EVEN))
        }
    }
}
