package example.com.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import example.com.data.model.exception.AuthorizationException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import java.util.*
import java.util.concurrent.TimeUnit

val EXPIRE_TIME = TimeUnit.DAYS.toMillis(180L)
private const val CLAIM_KEY = "id"
private const val ADMIN_KEY = "admin"
private const val jwtAudience = "jwt-audience"
private const val jwtDomain = "https://jwt-provider-domain/"
private const val jwtSecret = "\$S-d{xu-HG2V5OzuVDH~@rg1=lz+!V\$mjOBD\$Q%Xa#hptvI\$!?wf}_"

fun Application.configureSecurity() {
    val jwtRealm = "ParsPa-AI"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun createToken(id: Long): String? {
    return JWT
        .create()
        .withAudience(jwtAudience)
        .withIssuer(jwtDomain)
        .withClaim(CLAIM_KEY, id)
        .withExpiresAt(
            Date(
                System
                    .currentTimeMillis()
                    .plus(EXPIRE_TIME)
            )
        )
        .sign(Algorithm.HMAC256(jwtSecret))
}

fun createAdminToken(): String? {
    return JWT
        .create()
        .withAudience(jwtAudience)
        .withIssuer(jwtDomain)
        .withClaim(ADMIN_KEY, true)
        .sign(Algorithm.HMAC256(jwtSecret))
}

fun PipelineContext<Unit, ApplicationCall>.getIdFromToken(): Long {
    val principal = call.principal<JWTPrincipal>()
    return principal?.payload
        ?.getClaim(CLAIM_KEY)
        ?.asLong() ?: throw AuthorizationException()
}

fun PipelineContext<Unit, ApplicationCall>.getPathParameter(path: String) = call.parameters[path]
fun PipelineContext<Unit, ApplicationCall>.getHeader(header: String) = call.request.header(header)
fun PipelineContext<Unit, ApplicationCall>.getFlavorHeader() =
    Flavor.valueOf((call.request.header("flavor") ?: "persian").uppercase())

enum class Flavor {
    PERSIAN, NATIVE;

    val isPersian
        get() = this == PERSIAN
}