package re.alwyn974.inventory.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import re.alwyn974.inventory.config.AppConfig
import re.alwyn974.inventory.model.UserRole
import java.util.*

class JwtService(private val config: AppConfig) {
    private val validityInMs = 36_000_00 * 24 * 7 // 1 week

    val algorithm: Algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun generateToken(userId: String, username: String, role: UserRole): String {
        return JWT.create()
            .withSubject(userId)
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }

    fun verifyToken(token: String): TokenPayload? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(config.jwtIssuer)
                .withAudience(config.jwtAudience)
                .build()

            val jwt = verifier.verify(token)
            TokenPayload(
                userId = jwt.subject,
                username = jwt.getClaim("username").asString(),
                role = UserRole.valueOf(jwt.getClaim("role").asString())
            )
        } catch (e: JWTVerificationException) {
            null
        }
    }
}

data class TokenPayload(
    val userId: String,
    val username: String,
    val role: UserRole
)
