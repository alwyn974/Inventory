package re.alwyn974.inventory.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import re.alwyn974.inventory.shared.model.UserDto

@Serializable
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

expect class PlatformStorage() {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
    fun clear()
}
