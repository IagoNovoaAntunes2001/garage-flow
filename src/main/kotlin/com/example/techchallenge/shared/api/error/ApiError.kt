package com.example.techchallenge.shared.api.error

import java.time.Instant

data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String,
    val correlationId: String,
    val violations: List<FieldViolation> = emptyList(),
)

data class FieldViolation(
    val field: String,
    val message: String,
)
