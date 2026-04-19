package kupio.mobile.features.auth.data

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()
