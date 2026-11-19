package com.finance.infraestructure.seeder

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource

class JointAccountSchemaMigrationRunnerTest {

    @Test
    fun `should drop NOT NULL on accounts owner_id when PostgreSQL and column is NOT NULL`() {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val metaData = mock(DatabaseMetaData::class.java)
        val jdbcTemplate = mock(JdbcTemplate::class.java)

        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("PostgreSQL")
        `when`(jdbcTemplate.queryForObject(anyString(), eq(Long::class.java))).thenReturn(1L)

        val runner = JointAccountSchemaMigrationRunner(dataSource, jdbcTemplate)
        runner.run()

        verify(jdbcTemplate).execute("ALTER TABLE accounts ALTER COLUMN owner_id DROP NOT NULL")
    }

    @Test
    fun `should be idempotent when already nullable or when running on H2`() {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val metaData = mock(DatabaseMetaData::class.java)
        val jdbcTemplate = mock(JdbcTemplate::class.java)

        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("H2")

        val runner = JointAccountSchemaMigrationRunner(dataSource, jdbcTemplate)
        runner.run()

        verify(jdbcTemplate, never()).execute(anyString())

        `when`(metaData.databaseProductName).thenReturn("PostgreSQL")
        `when`(jdbcTemplate.queryForObject(anyString(), eq(Long::class.java))).thenReturn(0L)
        runner.run()

        verify(jdbcTemplate, never()).execute(anyString())
    }
}
