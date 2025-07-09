package re.alwyn974.inventory.service

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import re.alwyn974.inventory.model.RefreshTokens
import re.alwyn974.inventory.model.Users
import java.security.SecureRandom
import java.util.*
import kotlin.time.Duration.Companion.days

class RefreshTokenService {
    private val tokenValidityDays = 30L
    private val secureRandom = SecureRandom()

    fun generateRefreshToken(userId: UUID, deviceInfo: String? = null): String {
        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        val token = Base64.getEncoder().encodeToString(tokenBytes)

        val expiresAt = Clock.System.now().plus(tokenValidityDays.days).toLocalDateTime(TimeZone.currentSystemDefault())

        transaction {
            // Revoke all existing tokens for this user to enforce single session
            RefreshTokens.update({ RefreshTokens.user eq userId }) {
                it[isRevoked] = true
            }

            // Insert new refresh token
            RefreshTokens.insert {
                it[user] = userId
                it[RefreshTokens.token] = token
                it[RefreshTokens.expiresAt] = expiresAt
                it[RefreshTokens.deviceInfo] = deviceInfo
            }
        }

        return token
    }

    fun validateAndUseRefreshToken(token: String): UUID? {
        return transaction {
            val refreshTokenRow = RefreshTokens
                .join(Users, JoinType.INNER, RefreshTokens.user, Users.id)
                .selectAll()
                .where {
                    RefreshTokens.token eq token and not(RefreshTokens.isRevoked) and
                    Users.isActive and
                    (RefreshTokens.expiresAt greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                }
                .singleOrNull()

            if (refreshTokenRow != null) {
                val userId = refreshTokenRow[RefreshTokens.user].value

                // Update last used timestamp
                RefreshTokens.update({ RefreshTokens.token eq token }) {
                    it[lastUsedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }

                userId
            } else {
                null
            }
        }
    }

    fun revokeRefreshToken(token: String) {
        transaction {
            RefreshTokens.update({ RefreshTokens.token eq token }) {
                it[isRevoked] = true
            }
        }
    }

    fun revokeAllUserTokens(userId: UUID) {
        transaction {
            RefreshTokens.update({ RefreshTokens.user eq userId }) {
                it[isRevoked] = true
            }
        }
    }

    fun cleanupExpiredTokens() {
        transaction {
            RefreshTokens.deleteWhere {
                expiresAt less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }
}
