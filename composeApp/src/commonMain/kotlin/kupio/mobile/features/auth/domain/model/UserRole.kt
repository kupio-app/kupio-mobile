package kupio.mobile.features.auth.domain.model

enum class UserRole {
    USER,
    MODERATOR,
    ADMIN,
    UNKNOWN;

    val canModerate: Boolean
        get() = this == MODERATOR || this == ADMIN

    companion object {
        fun fromString(raw: String): UserRole = when (raw.lowercase()) {
            "user" -> USER
            "moderator" -> MODERATOR
            "admin" -> ADMIN
            else -> UNKNOWN
        }
    }
}
