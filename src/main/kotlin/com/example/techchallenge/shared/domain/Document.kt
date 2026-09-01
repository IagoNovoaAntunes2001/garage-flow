package com.example.techchallenge.shared.domain

sealed interface Document {
    val value: String
    val type: DocumentType

    fun masked(): String

    companion object {
        fun from(raw: String): Document {
            if (raw.any { !it.isAsciiDigit() && it !in DOCUMENT_FORMATTING_CHARACTERS }) {
                invalidDocument()
            }
            val normalized = raw.filter(Char::isAsciiDigit)
            return when (normalized.length) {
                CPF_LENGTH -> Cpf.from(normalized)
                CNPJ_LENGTH -> Cnpj.from(normalized)
                else -> invalidDocument()
            }
        }

        private const val CPF_LENGTH = 11
        private const val CNPJ_LENGTH = 14
        private val DOCUMENT_FORMATTING_CHARACTERS = setOf('.', '-', '/', ' ', '\t', '\r', '\n')
    }
}

enum class DocumentType {
    CPF,
    CNPJ,
}

@JvmInline
value class Cpf private constructor(
    override val value: String,
) : Document {
    override val type: DocumentType get() = DocumentType.CPF

    override fun masked(): String = "***.${value.substring(3, 6)}.${value.substring(6, 9)}-**"

    companion object {
        fun from(normalized: String): Cpf {
            if (!isValidCpf(normalized)) invalidDocument()
            return Cpf(normalized)
        }
    }
}

@JvmInline
value class Cnpj private constructor(
    override val value: String,
) : Document {
    override val type: DocumentType get() = DocumentType.CNPJ

    override fun masked(): String =
        "**.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8, 12)}-**"

    companion object {
        fun from(normalized: String): Cnpj {
            if (!isValidCnpj(normalized)) invalidDocument()
            return Cnpj(normalized)
        }
    }
}

private fun isValidCpf(value: String): Boolean {
    if (value.length != 11 || value.toSet().size == 1) return false
    val digits = value.map(Char::digitToInt)
    val first = calculateDigit(digits.take(9), (10 downTo 2).toList())
    val second = calculateDigit(digits.take(9) + first, (11 downTo 2).toList())
    return digits[9] == first && digits[10] == second
}

private fun isValidCnpj(value: String): Boolean {
    if (value.length != 14 || value.toSet().size == 1) return false
    val digits = value.map(Char::digitToInt)
    val first = calculateDigit(digits.take(12), listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
    val second = calculateDigit(digits.take(12) + first, listOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
    return digits[12] == first && digits[13] == second
}

private fun calculateDigit(digits: List<Int>, weights: List<Int>): Int {
    val remainder = digits.zip(weights).sumOf { (digit, weight) -> digit * weight } % 11
    return if (remainder < 2) 0 else 11 - remainder
}

private fun invalidDocument(): Nothing = throw DomainValidationException(
    ErrorCode.INVALID_DOCUMENT,
    "Invalid CPF or CNPJ document",
)

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
