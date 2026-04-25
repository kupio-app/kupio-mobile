package kupio.mobile.features.auth.data.repository

import kupio.mobile.core.datetime.nowEpochSeconds as defaultNowEpochSeconds

fun interface AuthClock {
    fun nowEpochSeconds(): Long
}

class SystemAuthClock : AuthClock {
    override fun nowEpochSeconds(): Long = defaultNowEpochSeconds()
}
