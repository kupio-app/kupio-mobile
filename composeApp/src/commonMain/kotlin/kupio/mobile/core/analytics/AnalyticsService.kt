package kupio.mobile.core.analytics

interface AnalyticsService {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun setUserId(userId: String?)
    fun recordException(throwable: Throwable, context: Map<String, String> = emptyMap())
    fun log(message: String)
}
