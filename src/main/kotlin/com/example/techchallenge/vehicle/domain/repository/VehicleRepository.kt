package com.example.techchallenge.vehicle.domain.repository

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.LicensePlate
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.vehicle.domain.model.Vehicle

data class VehiclePage(
    val content: List<Vehicle>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int,
)

interface VehicleRepository {
    fun save(vehicle: Vehicle): Vehicle
    fun findById(id: VehicleId): Vehicle?
    fun existsByLicensePlate(licensePlate: LicensePlate): Boolean
    fun list(licensePlate: LicensePlate?, page: Int, size: Int): VehiclePage
    fun listByCustomer(customerId: CustomerId, page: Int, size: Int): VehiclePage
    fun isReferenced(id: VehicleId): Boolean
    fun delete(vehicle: Vehicle)
}
