package com.example.techchallenge.authentication.infrastructure.persistence

import com.example.techchallenge.authentication.application.Administrator
import com.example.techchallenge.authentication.application.AdministratorRepository
import com.example.techchallenge.authentication.application.AdministratorRole
import com.example.techchallenge.shared.domain.AdministratorId
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "administrators")
class AdministratorEntity(
    @Id
    val id: UUID,
    @Column(nullable = false, unique = true, length = 120)
    val username: String,
    @Column(name = "password_hash", nullable = false, length = 100)
    val passwordHash: String,
    @Column(nullable = false)
    val active: Boolean,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "administrator_roles", joinColumns = [JoinColumn(name = "administrator_id")])
    @Column(name = "role", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    val roles: Set<AdministratorRole>,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Version
    @Column(nullable = false)
    val version: Long = 0,
)

interface SpringDataAdministratorRepository : JpaRepository<AdministratorEntity, UUID> {
    fun findByUsername(username: String): AdministratorEntity?
    fun existsByUsername(username: String): Boolean
}

@Repository
class AdministratorPersistenceAdapter(
    private val repository: SpringDataAdministratorRepository,
) : AdministratorRepository {
    override fun findByUsername(username: String): Administrator? =
        repository.findByUsername(Administrator.normalizeUsername(username))?.toDomain()

    override fun existsByUsername(username: String): Boolean =
        repository.existsByUsername(Administrator.normalizeUsername(username))

    override fun count(): Long = repository.count()

    override fun save(administrator: Administrator): Administrator =
        repository.save(administrator.toEntity()).toDomain()

    private fun AdministratorEntity.toDomain() = Administrator.restore(
        id = AdministratorId(id),
        username = username,
        passwordHash = passwordHash,
        active = active,
        roles = roles,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

    private fun Administrator.toEntity() = AdministratorEntity(
        id = id.value,
        username = username,
        passwordHash = passwordHash,
        active = active,
        roles = roles,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )
}
