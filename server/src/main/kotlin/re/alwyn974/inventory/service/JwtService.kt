package re.alwyn974.inventory.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import re.alwyn974.inventory.model.UserRole
import java.util.*

object JwtService {
    private const val SECRET = "your-secret-key-change-in-production"
    const val ISSUER = "inventory-app"
    private const val VALIDITY_IN_MS = 36_000_00 * 24 * 7 // 1 week

    val algorithm: Algorithm? = Algorithm.HMAC256(SECRET)

    fun generateToken(userId: String, username: String, role: UserRole): String {
        return JWT.create()
            .withSubject(userId)
            .withIssuer(ISSUER)
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
            .sign(algorithm)
    }

    fun verifyToken(token: String): TokenPayload? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
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
