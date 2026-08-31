package com.example.techchallenge.vehicle.domain

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.LicensePlate
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.shared.domain.VehicleYear
import com.example.techchallenge.vehicle.domain.model.Vehicle
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class VehicleTest {
    private val now = Instant.parse("2026-08-31T12:00:00Z")
    private val ownerId = CustomerId.new()

    @Test
    fun `creates an active vehicle owned by one customer with normalized plate`() {
        val vehicle = Vehicle.create(
            VehicleId.new(), ownerId, LicensePlate.from("abc-1d23"), " Ford ", " Ka ", VehicleYear.of(2020, 2026), now,
        )
        assertThat(vehicle.customerId).isEqualTo(ownerId)
        assertThat(vehicle.licensePlate.value).isEqualTo("ABC1D23")
        assertThat(vehicle.brand).isEqualTo("Ford")
        assertThat(vehicle.model).isEqualTo("Ka")
        assertThat(vehicle.active).isTrue()
    }

    @Test
    fun `updates mutable vehicle data without changing ownership or creation identity`() {
        val vehicle = validVehicle()
        val changed = vehicle.update(LicensePlate.from("DEF2G34"), "Honda", "Fit", VehicleYear.of(2021, 2026), now.plusSeconds(60))
        assertThat(changed.id).isEqualTo(vehicle.id)
        assertThat(changed.customerId).isEqualTo(ownerId)
        assertThat(changed.licensePlate.value).isEqualTo("DEF2G34")
        assertThat(changed.createdAt).isEqualTo(now)
    }

    @Test
    fun `rejects blank brand or model and invalid year`() {
        assertThatThrownBy { validVehicle().update(LicensePlate.from("ABC1D23"), " ", "Ka", VehicleYear.of(2020, 2026), now) }
            .isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { VehicleYear.of(2028, 2026) }.isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    fun `deactivates while preserving vehicle ownership`() {
        val vehicle = validVehicle()
        val deactivated = vehicle.deactivate(now.plusSeconds(30))
        assertThat(deactivated.active).isFalse()
        assertThat(deactivated.customerId).isEqualTo(ownerId)
    }

    private fun validVehicle() = Vehicle.create(
        VehicleId.new(), ownerId, LicensePlate.from("ABC1D23"), "Ford", "Ka", VehicleYear.of(2020, 2026), now,
    )
}
