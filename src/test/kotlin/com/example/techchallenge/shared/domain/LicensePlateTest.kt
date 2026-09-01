package com.example.techchallenge.shared.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LicensePlateTest {
    @ParameterizedTest
    @ValueSource(strings = ["ABC-1234", "abc1234", "ABC1D23", "abc-1d23"])
    fun `normalizes valid legacy and Mercosur plates`(raw: String) {
        assertThat(LicensePlate.from(raw).value).isIn("ABC1234", "ABC1D23")
    }

    @ParameterizedTest
    @ValueSource(strings = ["AB12345", "ABC123", "ABC12D3", "ABC_1234", ""])
    fun `rejects invalid Brazilian plates`(raw: String) {
        assertThatThrownBy { LicensePlate.from(raw) }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("plate")
    }

    @ParameterizedTest
    @ValueSource(ints = [1885, 3000])
    fun `rejects implausible vehicle years`(year: Int) {
        assertThatThrownBy { VehicleYear.of(year, currentYear = 2026) }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("year")
    }
}
