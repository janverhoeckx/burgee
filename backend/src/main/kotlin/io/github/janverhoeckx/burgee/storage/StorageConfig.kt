package io.github.janverhoeckx.burgee.storage

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.origin.OriginLookup
import org.springframework.boot.origin.OriginTrackedResource
import org.springframework.boot.origin.TextResourceOrigin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource
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
    fun inMemoryDataSource(environment: ConfigurableEnvironment): DataSource {
        explicitDatabaseUrlSettingName(environment)?.let { setting ->
            throw IllegalStateException(
                "BURGEE_STORAGE=memory cannot be combined with $setting: remove $setting to run in memory, " +
                    "or set BURGEE_STORAGE=postgres to use that database",
            )
        }
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

    /**
     * Names the setting that points Burgee at a real database, or null when the only datasource URL is the
     * built-in default from the application.yml bundled in the jar.
     */
    private fun explicitDatabaseUrlSettingName(environment: ConfigurableEnvironment): String? {
        if (environment.containsProperty("DB_URL")) return "DB_URL"
        val source = environment.propertySources
            .filterNot(ConfigurationPropertySources::isAttachedConfigurationPropertySource)
            .firstOrNull { it.containsProperty(DATASOURCE_URL) } ?: return null
        val origin = OriginLookup.getOrigin(source, DATASOURCE_URL) as? TextResourceOrigin
        val resource = origin?.resource.let { (it as? OriginTrackedResource)?.resource ?: it }
        val fromBundledConfig = (resource as? ClassPathResource)?.path == "application.yml"
        return if (fromBundledConfig) null else DATASOURCE_URL
    }

    companion object {
        private const val DATASOURCE_URL = "spring.datasource.url"

        private val log = LoggerFactory.getLogger(StorageConfig::class.java)

        // KEY is an H2 keyword but a column name here, and H2 lacks Postgres' TIMESTAMPTZ alias, so the
        // unchanged Flyway migrations need both taken care of on the connection.
        private const val IN_MEMORY_URL =
            "jdbc:h2:mem:burgee;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;" +
                "NON_KEYWORDS=KEY;DB_CLOSE_DELAY=-1;" +
                "INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE"
    }
}
