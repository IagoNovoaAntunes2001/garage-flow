package com.example.techchallenge.support

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class DatabaseMigrationIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `applies the initial migration to an empty PostgreSQL database`() {
        val applied = flyway.info().applied()

        assertThat(applied).hasSize(1)
        assertThat(applied.single().version.toString()).isEqualTo("1")

        val businessTables = jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name <> 'flyway_schema_history'
            ORDER BY table_name
            """.trimIndent(),
            String::class.java,
        )

        assertThat(businessTables).containsExactlyInAnyOrder(
            "administrator_roles",
            "administrators",
            "approvals",
            "catalog_services",
            "customers",
            "inventory_items",
            "inventory_movements",
            "quotation_lines",
            "quotations",
            "service_order_items",
            "service_order_status_history",
            "service_orders",
            "vehicles",
        )
    }

    @Test
    fun `database constraints reject invalid stock`() {
        val failure = runCatching {
            jdbcTemplate.update(
                """
                INSERT INTO inventory_items (
                    id, item_type, name, description, unit_price, available_quantity,
                    active, created_at, updated_at, version
                ) VALUES (?::uuid, 'PART', 'Invalid', 'Invalid', 10.00, -1, true, now(), now(), 0)
                """.trimIndent(),
                "00000000-0000-0000-0000-000000000001",
            )
        }

        assertThat(failure.exceptionOrNull()).isNotNull()
    }
}
