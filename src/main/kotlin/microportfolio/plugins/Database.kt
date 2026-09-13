package microportfolio.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.log
import microportfolio.domain.Orders
import microportfolio.domain.Portfolios
import microportfolio.domain.Users
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabase() {
    val config = environment.config.config("database")
    val url = config.property("url").getString()
    val user = config.property("user").getString()
    val password = config.property("password").getString()

    val hikariConfig = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = url
        username = user
        this.password = password
        maximumPoolSize = 5
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    Database.connect(HikariDataSource(hikariConfig))

    // Create tables if they don't exist (dev only — use Flyway in production)
    transaction {
        SchemaUtils.create(Users, Portfolios, Orders)
    }

    log.info("Database connected and tables created")
}