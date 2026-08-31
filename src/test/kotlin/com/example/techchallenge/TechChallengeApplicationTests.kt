package com.example.techchallenge

import com.example.techchallenge.authentication.infrastructure.persistence.SpringDataAdministratorRepository
import com.example.techchallenge.catalog.infrastructure.persistence.SpringDataCatalogServiceRepository
import com.example.techchallenge.customer.infrastructure.persistence.SpringDataCustomerRepository
import com.example.techchallenge.inventory.infrastructure.persistence.SpringDataInventoryItemRepository
import com.example.techchallenge.inventory.infrastructure.persistence.SpringDataInventoryMovementRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataApprovalRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataQuotationLineRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataQuotationRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataServiceOrderItemRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataServiceOrderRepository
import com.example.techchallenge.serviceorder.infrastructure.persistence.SpringDataStatusHistoryRepository
import jakarta.persistence.EntityManager
import com.example.techchallenge.vehicle.infrastructure.persistence.SpringDataVehicleRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [TechChallengeApplication::class],
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "garage-flow.security.jwt.secret=01234567890123456789012345678901",
    ],
)
class TechChallengeApplicationTests {
    @MockitoBean
    private lateinit var administratorRepository: SpringDataAdministratorRepository

    @MockitoBean
    private lateinit var customerRepository: SpringDataCustomerRepository

    @MockitoBean
    private lateinit var catalogServiceRepository: SpringDataCatalogServiceRepository

    @MockitoBean
    private lateinit var inventoryItemRepository: SpringDataInventoryItemRepository

    @MockitoBean
    private lateinit var inventoryMovementRepository: SpringDataInventoryMovementRepository

    @MockitoBean
    private lateinit var serviceOrderRepository: SpringDataServiceOrderRepository

    @MockitoBean
    private lateinit var serviceOrderItemRepository: SpringDataServiceOrderItemRepository

    @MockitoBean
    private lateinit var quotationRepository: SpringDataQuotationRepository

    @MockitoBean
    private lateinit var quotationLineRepository: SpringDataQuotationLineRepository

    @MockitoBean
    private lateinit var approvalRepository: SpringDataApprovalRepository

    @MockitoBean
    private lateinit var statusHistoryRepository: SpringDataStatusHistoryRepository

    @MockitoBean
    private lateinit var entityManager: EntityManager

    @MockitoBean
    private lateinit var vehicleRepository: SpringDataVehicleRepository

    @Test
    fun contextLoads() = Unit
}
