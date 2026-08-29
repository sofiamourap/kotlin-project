package microportfolio

import microportfolio.plugins.configureSecurity
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    /**
     * testApplication does NOT read application.yaml on its own - it starts
     * with an empty config - so we point it at the test config explicitly and
     * install everything Application.module() does except the database.
     */
    private fun ApplicationTestBuilder.installTestApp() {
        environment {
            config = ApplicationConfig("test-application.yaml")
        }
        application {
            configureSerialization()
            configureStatusPages()
            configureSecurity()
            configureRouting()
        }
    }

    @Test
    fun `health endpoint returns UP`() = testApplication {
        installTestApp()
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

    @Test
    fun `portfolio requires a token`() = testApplication {
        installTestApp()
        assertEquals(HttpStatusCode.Unauthorized, client.get("/portfolio").status)
    }

    @Test
    fun `login issues a token that opens portfolio`() = testApplication {
        installTestApp()

        val login = client.post("/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"email":"me@example.com","password":"irrelevant-for-now"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val token = Json.decodeFromString<LoginResponse>(login.bodyAsText()).token
        val portfolio = client.get("/portfolio") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, portfolio.status)
    }
}
