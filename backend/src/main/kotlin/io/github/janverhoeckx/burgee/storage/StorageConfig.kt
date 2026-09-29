package io.github.janverhoeckx.burgee.storage

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {

    /**
     * In-memory storage mode: an embedded H2 database in PostgreSQL mode that lives as long as the process
     * (see docs/adr/0001-h2-for-in-memory-storage.md). Defining the DataSource makes Spring Boot skip its own,
     * so the configured Postgres URL is never used. Flyway runs the regular migrations against it.
     */
    @Bean
    @ConditionalOnProperty(name = ["burgee.storage"], havingValue = "memory")
    fun inMemoryDataSource(): DataSource {
        log.warn("Storage mode is in-memory: all flags, users and audit entries are lost on restart")
        return HikariDataSource().apply {
            jdbcUrl = IN_MEMORY_URL
            username = "sa"
            password = ""
        }
    }

    /**
     * Makes Spring Data JDBC generate the same (lower-case, quoted) SQL as it does against Postgres, instead of
     * detecting H2 and upper-casing unannotated column names.
     */
    @Bean
    @ConditionalOnProperty(name = ["burgee.storage"], havingValue = "memory")
    fun inMemoryJdbcDialect(): JdbcDialect = JdbcPostgresDialect.INSTANCE

    companion object {
        private val log = LoggerFactory.getLogger(StorageConfig::class.java)

        // KEY is an H2 keyword but a column name here, and H2 lacks Postgres' TIMESTAMPTZ alias, so the
        // unchanged Flyway migrations need both taken care of on the connection.
        private const val IN_MEMORY_URL =
            "jdbc:h2:mem:burgee;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;" +
                "NON_KEYWORDS=KEY;DB_CLOSE_DELAY=-1;" +
                "INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE"
    }
}
