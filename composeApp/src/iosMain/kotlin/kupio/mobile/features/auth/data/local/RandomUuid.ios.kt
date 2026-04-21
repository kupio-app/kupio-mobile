package kupio.mobile.features.auth.data.local

import platform.Foundation.NSUUID

actual fun randomUuid(): String = NSUUID().UUIDString()
