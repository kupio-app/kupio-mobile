package kupio.mobile.features.auth.data.local

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()
