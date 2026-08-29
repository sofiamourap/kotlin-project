package microportfolio.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val config = environment.config.config("database")

    val hikariConfig = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = config.property("url").getString()
        username = config.property("user").getString()
        password = config.property("password").getString()
        maximumPoolSize = 5
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    Database.connect(HikariDataSource(hikariConfig))
    log.info("Database pool initialised")
    // TODO: create tables here (SchemaUtils.create(...)) or run Flyway migrations.
}
