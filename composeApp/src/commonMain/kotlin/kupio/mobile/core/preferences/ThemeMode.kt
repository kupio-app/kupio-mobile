package kupio.mobile.core.preferences

enum class ThemeMode(
    val storageValue: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

fun ThemeMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean {
    return when (this) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
