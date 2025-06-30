package re.alwyn974.inventory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform