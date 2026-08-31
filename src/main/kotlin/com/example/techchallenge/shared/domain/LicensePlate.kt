package com.example.techchallenge.shared.domain

import java.time.Year

@JvmInline
value class LicensePlate private constructor(
    val value: String,
) {
    companion object {
        private val legacyPattern = Regex("^[A-Z]{3}[0-9]{4}$")
        private val mercosurPattern = Regex("^[A-Z]{3}[0-9][A-Z][0-9]{2}$")

        fun from(raw: String): LicensePlate {
            val normalized = raw.trim().uppercase().replace("-", "").replace(" ", "")
            if (!legacyPattern.matches(normalized) && !mercosurPattern.matches(normalized)) {
                throw DomainValidationException(
                    ErrorCode.INVALID_LICENSE_PLATE,
                    "Invalid Brazilian license plate",
                )
            }
            return LicensePlate(normalized)
        }
    }
}

@JvmInline
value class VehicleYear private constructor(
    val value: Int,
) {
    companion object {
        fun of(year: Int, currentYear: Int = Year.now().value): VehicleYear {
            if (year !in FIRST_AUTOMOBILE_YEAR..(currentYear + 1)) {
                throw DomainValidationException(
                    ErrorCode.INVALID_VEHICLE_YEAR,
                    "Vehicle year must be between $FIRST_AUTOMOBILE_YEAR and ${currentYear + 1}",
                )
            }
            return VehicleYear(year)
        }

        private const val FIRST_AUTOMOBILE_YEAR = 1886
    }
}
