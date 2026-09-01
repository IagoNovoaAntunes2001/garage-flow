package com.example.techchallenge.vehicle.domain.model

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import com.example.techchallenge.shared.domain.LicensePlate
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.shared.domain.VehicleYear
import java.time.Instant

class Vehicle private constructor(
    val id: VehicleId,
    val customerId: CustomerId,
    val licensePlate: LicensePlate,
    val brand: String,
    val model: String,
    val year: VehicleYear,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
) {
    fun update(licensePlate: LicensePlate, brand: String, model: String, year: VehicleYear, now: Instant) =
        Vehicle(id, customerId, licensePlate, validText(brand, 80, "brand"), validText(model, 100, "model"), year, active, createdAt, now, version)

    fun deactivate(now: Instant) = Vehicle(id, customerId, licensePlate, brand, model, year, false, createdAt, now, version)

    companion object {
        fun create(
            id: VehicleId,
            customerId: CustomerId,
            licensePlate: LicensePlate,
            brand: String,
            model: String,
            year: VehicleYear,
            now: Instant,
        ) = Vehicle(id, customerId, licensePlate, validText(brand, 80, "brand"), validText(model, 100, "model"), year, true, now, now, 0)

        fun restore(
            id: VehicleId, customerId: CustomerId, licensePlate: LicensePlate, brand: String, model: String,
            year: VehicleYear, active: Boolean, createdAt: Instant, updatedAt: Instant, version: Long,
        ) = Vehicle(id, customerId, licensePlate, brand, model, year, active, createdAt, updatedAt, version)

        private fun validText(value: String, max: Int, field: String): String = value.trim().also {
            if (it.isBlank() || it.length > max) throw DomainValidationException(
                ErrorCode.INVALID_VEHICLE, "Vehicle $field must contain between 1 and $max characters",
            )
        }
    }
}
