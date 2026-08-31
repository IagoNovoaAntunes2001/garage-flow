package com.example.techchallenge.vehicle.infrastructure.persistence

import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.LicensePlate
import com.example.techchallenge.shared.domain.VehicleId
import com.example.techchallenge.shared.domain.VehicleYear
import com.example.techchallenge.vehicle.domain.model.Vehicle
import com.example.techchallenge.vehicle.domain.repository.VehiclePage
import com.example.techchallenge.vehicle.domain.repository.VehicleRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vehicles")
class VehicleEntity(
    @Id val id: UUID,
    @Column(name = "customer_id", nullable = false) val customerId: UUID,
    @Column(name = "license_plate", nullable = false, unique = true, length = 7) val licensePlate: String,
    @Column(nullable = false, length = 80) val brand: String,
    @Column(nullable = false, length = 100) val model: String,
    @Column(name = "production_year", nullable = false) val year: Int,
    @Column(nullable = false) val active: Boolean,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Version @Column(nullable = false) val version: Long = 0,
)

interface SpringDataVehicleRepository : JpaRepository<VehicleEntity, UUID> {
    fun existsByLicensePlate(licensePlate: String): Boolean
    fun findAllByLicensePlate(licensePlate: String, pageable: Pageable): Page<VehicleEntity>
    fun findAllByCustomerId(customerId: UUID, pageable: Pageable): Page<VehicleEntity>
}

@Repository
class VehiclePersistenceAdapter(
    private val repository: SpringDataVehicleRepository,
    private val entityManager: EntityManager,
) : VehicleRepository {
    override fun save(vehicle: Vehicle) = repository.save(vehicle.toEntity()).toDomain()
    override fun findById(id: VehicleId) = repository.findById(id.value).orElse(null)?.toDomain()
    override fun existsByLicensePlate(licensePlate: LicensePlate) = repository.existsByLicensePlate(licensePlate.value)

    override fun list(licensePlate: LicensePlate?, page: Int, size: Int): VehiclePage {
        val request = pageRequest(page, size)
        return (if (licensePlate == null) repository.findAll(request) else repository.findAllByLicensePlate(licensePlate.value, request)).toDomainPage()
    }

    override fun listByCustomer(customerId: CustomerId, page: Int, size: Int) =
        repository.findAllByCustomerId(customerId.value, pageRequest(page, size)).toDomainPage()

    override fun isReferenced(id: VehicleId): Boolean = (entityManager.createNativeQuery(
        "SELECT count(*) FROM service_orders WHERE vehicle_id = :id",
    ).setParameter("id", id.value).singleResult as Number).toLong() > 0

    override fun delete(vehicle: Vehicle) = repository.deleteById(vehicle.id.value)

    private fun pageRequest(page: Int, size: Int) = org.springframework.data.domain.PageRequest.of(
        page, size, org.springframework.data.domain.Sort.by("licensePlate", "id"),
    )
    private fun Page<VehicleEntity>.toDomainPage() = VehiclePage(content.map { it.toDomain() }, number, size, totalElements, totalPages)
    private fun VehicleEntity.toDomain() = Vehicle.restore(
        VehicleId(id), CustomerId(customerId), LicensePlate.from(licensePlate), brand, model, VehicleYear.of(year), active, createdAt, updatedAt, version,
    )
    private fun Vehicle.toEntity() = VehicleEntity(
        id.value, customerId.value, licensePlate.value, brand, model, year.value, active, createdAt, updatedAt, version,
    )
}
