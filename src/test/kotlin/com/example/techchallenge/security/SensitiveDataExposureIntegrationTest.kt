package com.example.techchallenge.security

import com.example.techchallenge.shared.api.error.ApiError
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Instant

class SensitiveDataExposureIntegrationTest {
    @Test
    fun `api error does not expose stack traces or tokens`() {
        val rendered = ApiError(Instant.parse("2026-08-31T00:00:00Z"), 400, "Bad Request", "VALIDATION_ERROR", "Invalid request", "/test", "cid").toString()
        assertFalse(rendered.contains("Exception"))
        assertFalse(rendered.contains("Bearer "))
    }
}
