package kupio.mobile.features.auth.data.repository

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun interface AuthClock {
    fun nowEpochSeconds(): Long
}

class SystemAuthClock : AuthClock {
    @OptIn(ExperimentalTime::class)
    override fun nowEpochSeconds(): Long {
        return Clock.System.now().epochSeconds
    }
}
