package microportfolio.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

object Users : UuidTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

/** API shape — never include the password hash. */
@Serializable
data class UserResponse(val id: String, val email: String, val createdAt: String)
