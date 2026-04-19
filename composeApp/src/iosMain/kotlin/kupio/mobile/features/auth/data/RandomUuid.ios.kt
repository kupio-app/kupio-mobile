package kupio.mobile.features.auth.data

import platform.Foundation.NSUUID

actual fun randomUuid(): String = NSUUID().UUIDString()
