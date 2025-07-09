package re.alwyn974.inventory.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import re.alwyn974.inventory.config.AppConfig
import re.alwyn974.inventory.shared.model.UserRole
import java.util.*

class JwtService(private val config: AppConfig) {
    private val accessTokenValidityInMs = 15 * 60 * 1000L // 15 minutes
    private val refreshTokenValidityInMs = 30 * 24 * 60 * 60 * 1000L // 30 days

    val algorithm: Algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun generateAccessToken(userId: String, username: String, role: UserRole): String {
        return JWT.create()
            .withSubject(userId)
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenValidityInMs))
            .sign(algorithm)
    }

    fun verifyToken(token: String): TokenPayload? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(config.jwtIssuer)
                .withAudience(config.jwtAudience)
                .withClaim("type", "access")
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

    // Keep the old method for backward compatibility, but mark as deprecated
    @Deprecated("Use generateAccessToken instead", ReplaceWith("generateAccessToken(userId, username, role)"))
    fun generateToken(userId: String, username: String, role: UserRole): String {
        return generateAccessToken(userId, username, role)
    }
}

data class TokenPayload(
    val userId: String,
    val username: String,
    val role: UserRole
)
