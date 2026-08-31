package com.example.techchallenge.shared.domain

enum class ErrorCode {
    INVALID_DOCUMENT,
    INVALID_LICENSE_PLATE,
    INVALID_VEHICLE_YEAR,
    INVALID_MONEY,
    INVALID_PAGINATION,
    INVALID_CUSTOMER,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    BUSINESS_RULE_VIOLATION,
}

sealed class DomainException(
    val code: ErrorCode,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class DomainValidationException(
    code: ErrorCode,
    message: String,
) : DomainException(code, message)

class ResourceNotFoundException(
    message: String,
) : DomainException(ErrorCode.RESOURCE_NOT_FOUND, message)

class ConflictException(
    message: String,
) : DomainException(ErrorCode.CONFLICT, message)

class BusinessRuleException(
    message: String,
) : DomainException(ErrorCode.BUSINESS_RULE_VIOLATION, message)
