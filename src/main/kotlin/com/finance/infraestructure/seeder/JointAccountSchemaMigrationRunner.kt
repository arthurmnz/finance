package com.finance.infraestructure.seeder

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@Order(0)
class JointAccountSchemaMigrationRunner(
    private val dataSource: DataSource,
    private val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        val isPostgres = dataSource.connection.use { connection ->
            connection.metaData.databaseProductName.contains("PostgreSQL", ignoreCase = true)
        }
        if (!isPostgres) {
            return
        }

        val nonNullableCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'accounts'
              AND column_name = 'owner_id'
              AND is_nullable = 'NO'
            """.trimIndent(),
            Long::class.java
        ) ?: 0L

        if (nonNullableCount > 0L) {
            logger.info("Aplicando migração de schema: removendo NOT NULL de accounts.owner_id para suporte a contas conjuntas.")
            jdbcTemplate.execute("ALTER TABLE accounts ALTER COLUMN owner_id DROP NOT NULL")
        }
    }
}
