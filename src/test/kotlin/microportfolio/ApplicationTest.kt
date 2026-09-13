package microportfolio

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import microportfolio.plugins.JwtSettings
import microportfolio.plugins.configureSecurity
import microportfolio.plugins.createToken
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
    fun `orders requires a token`() = testApplication {
        installTestApp()
        assertEquals(HttpStatusCode.Unauthorized, client.post("/orders").status)
    }

    @Test
    fun `malformed userId in token returns 400`() = testApplication {
        installTestApp()
        val token = createToken(
            userId = "not-a-uuid",
            settings = JwtSettings.from(ApplicationConfig("test-application.yaml")),
        )
        val portfolio = client.get("/portfolio") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.BadRequest, portfolio.status)
    }
}
