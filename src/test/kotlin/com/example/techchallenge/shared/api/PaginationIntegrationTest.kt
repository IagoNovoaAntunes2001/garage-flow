package com.example.techchallenge.shared.api

import com.example.techchallenge.shared.domain.DomainValidationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PaginationIntegrationTest {
    @Test
    fun `enforces page size bounds`() {
        assertThrows(DomainValidationException::class.java) { PageRequestDto.of(-1, 20) }
        assertThrows(DomainValidationException::class.java) { PageRequestDto.of(0, 101) }
    }
}
