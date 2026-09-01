package com.example.techchallenge.vehicle.application.usecase

import com.example.techchallenge.customer.domain.repository.CustomerRepository
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.BusinessRuleException
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.LicensePlate
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.shared.domain.VehicleYear
import com.example.techchallenge.vehicle.domain.model.Vehicle
import com.example.techchallenge.vehicle.domain.repository.VehiclePage
import com.example.techchallenge.vehicle.domain.repository.VehicleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VehicleUseCases(
    private val vehicles: VehicleRepository,
    private val customers: CustomerRepository,
    private val clock: ClockProvider,
) {
    @Transactional
    fun register(customerId: CustomerId, plate: String, brand: String, model: String, year: Int): Vehicle {
        requireActiveCustomer(customerId)
        val licensePlate = LicensePlate.from(plate)
        if (vehicles.existsByLicensePlate(licensePlate)) duplicate()
        return vehicles.save(Vehicle.create(VehicleId.new(), customerId, licensePlate, brand, model, VehicleYear.of(year), clock.now()))
    }

    @Transactional(readOnly = true) fun get(id: VehicleId) = vehicles.findById(id) ?: notFound()
    @Transactional(readOnly = true) fun list(plate: String?, page: PageRequestDto) =
        vehicles.list(plate?.let(LicensePlate::from), page.page, page.size)
    @Transactional(readOnly = true) fun listByCustomer(customerId: CustomerId, page: PageRequestDto): VehiclePage {
        if (customers.findById(customerId) == null) throw ResourceNotFoundException("Customer not found")
        return vehicles.listByCustomer(customerId, page.page, page.size)
    }

    @Transactional
    fun update(id: VehicleId, plate: String, brand: String, model: String, year: Int): Vehicle {
        val current = vehicles.findById(id) ?: notFound()
        val normalizedPlate = LicensePlate.from(plate)
        if (normalizedPlate != current.licensePlate && vehicles.existsByLicensePlate(normalizedPlate)) duplicate()
        return vehicles.save(current.update(normalizedPlate, brand, model, VehicleYear.of(year), clock.now()))
    }

    @Transactional
    fun remove(id: VehicleId) {
        val current = vehicles.findById(id) ?: notFound()
        if (vehicles.isReferenced(id)) vehicles.save(current.deactivate(clock.now())) else vehicles.delete(current)
    }

    private fun requireActiveCustomer(id: CustomerId) {
        val customer = customers.findById(id) ?: throw ResourceNotFoundException("Customer not found")
        if (!customer.active) throw BusinessRuleException("Inactive customer cannot own a new vehicle")
    }
    private fun duplicate(): Nothing = throw ConflictException("A vehicle with this license plate already exists")
    private fun notFound(): Nothing = throw ResourceNotFoundException("Vehicle not found")
}
