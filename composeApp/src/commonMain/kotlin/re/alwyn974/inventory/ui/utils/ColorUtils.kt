package re.alwyn974.inventory.ui.utils

import androidx.compose.ui.graphics.Color

/**
 * Utility functions for color parsing that work across all platforms
 */
object ColorUtils {
    /**
     * Parse a hex color string to a Compose Color
     * Supports formats: #RGB, #RRGGBB, #AARRGGBB
     */
    fun parseColor(colorString: String): Color {
        return try {
            val cleanColor = colorString.removePrefix("#")
            when (cleanColor.length) {
                3 -> {
                    // #RGB format - expand to #RRGGBB
                    val r = cleanColor[0].toString().repeat(2)
                    val g = cleanColor[1].toString().repeat(2)
                    val b = cleanColor[2].toString().repeat(2)
                    Color(
                        red = r.toInt(16) / 255f,
                        green = g.toInt(16) / 255f,
                        blue = b.toInt(16) / 255f
                    )
                }
                6 -> {
                    // #RRGGBB format
                    val r = cleanColor.substring(0, 2).toInt(16)
                    val g = cleanColor.substring(2, 4).toInt(16)
                    val b = cleanColor.substring(4, 6).toInt(16)
                    Color(
                        red = r / 255f,
                        green = g / 255f,
                        blue = b / 255f
                    )
                }
                8 -> {
                    // #AARRGGBB format
                    val a = cleanColor.substring(0, 2).toInt(16)
                    val r = cleanColor.substring(2, 4).toInt(16)
                    val g = cleanColor.substring(4, 6).toInt(16)
                    val b = cleanColor.substring(6, 8).toInt(16)
                    Color(
                        alpha = a / 255f,
                        red = r / 255f,
                        green = g / 255f,
                        blue = b / 255f
                    )
                }
                else -> Color.Gray
            }
        } catch (e: Exception) {
            Color.Gray
        }
    }
}
