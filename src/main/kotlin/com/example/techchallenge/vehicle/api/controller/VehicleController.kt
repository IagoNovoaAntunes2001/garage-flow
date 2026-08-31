package com.example.techchallenge.vehicle.api.controller

import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.vehicle.application.usecase.VehicleUseCases
import com.example.techchallenge.vehicle.domain.model.Vehicle
import com.example.techchallenge.vehicle.domain.repository.VehiclePage
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

data class VehicleRequest(
    @field:NotBlank val licensePlate: String,
    @field:NotBlank @field:Size(max = 80) val brand: String,
    @field:NotBlank @field:Size(max = 100) val model: String,
    @field:Min(1886) @field:Max(3000) val year: Int,
)
data class VehicleResponse(
    val id: UUID, val customerId: UUID, val licensePlate: String, val brand: String, val model: String,
    val year: Int, val active: Boolean,
)
data class VehiclePageResponse(
    val content: List<VehicleResponse>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int,
)
private fun Vehicle.response() = VehicleResponse(id.value, customerId.value, licensePlate.value, brand, model, year.value, active)
private fun VehiclePage.response() = VehiclePageResponse(content.map(Vehicle::response), page, size, totalElements, totalPages)

@RestController
class VehicleController(private val useCases: VehicleUseCases) {
    @PostMapping("/api/v1/admin/customers/{customerId}/vehicles")
    fun create(@PathVariable customerId: UUID, @Valid @RequestBody request: VehicleRequest): ResponseEntity<VehicleResponse> {
        val result = useCases.register(CustomerId(customerId), request.licensePlate, request.brand, request.model, request.year).response()
        val location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/admin/vehicles/{id}").buildAndExpand(result.id).toUri()
        return ResponseEntity.created(location).body(result)
    }

    @GetMapping("/api/v1/admin/customers/{customerId}/vehicles")
    fun listByCustomer(@PathVariable customerId: UUID, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
        useCases.listByCustomer(CustomerId(customerId), PageRequestDto.of(page, size)).response()

    @GetMapping("/api/v1/admin/vehicles")
    fun list(@RequestParam(required = false) licensePlate: String?, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
        useCases.list(licensePlate, PageRequestDto.of(page, size)).response()

    @GetMapping("/api/v1/admin/vehicles/{vehicleId}")
    fun get(@PathVariable vehicleId: UUID) = useCases.get(VehicleId(vehicleId)).response()

    @PutMapping("/api/v1/admin/vehicles/{vehicleId}")
    fun update(@PathVariable vehicleId: UUID, @Valid @RequestBody request: VehicleRequest) =
        useCases.update(VehicleId(vehicleId), request.licensePlate, request.brand, request.model, request.year).response()

    @DeleteMapping("/api/v1/admin/vehicles/{vehicleId}")
    fun remove(@PathVariable vehicleId: UUID): ResponseEntity<Void> {
        useCases.remove(VehicleId(vehicleId)); return ResponseEntity.noContent().build()
    }
}
