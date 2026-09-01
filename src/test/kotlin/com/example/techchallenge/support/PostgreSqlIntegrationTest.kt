package com.example.techchallenge.support

import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
abstract class PostgreSqlIntegrationTest {
    @Autowired
    private lateinit var databaseCleaner: JdbcTemplate

    @BeforeEach
    fun cleanDatabaseBeforeIntegrationTest() {
        databaseCleaner.update(
            """
            TRUNCATE TABLE
                inventory_movements,
                approvals,
                quotation_lines,
                quotations,
                service_order_status_history,
                service_order_items,
                service_orders,
                inventory_items,
                catalog_services,
                vehicles,
                customers
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun configurePostgreSql(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
