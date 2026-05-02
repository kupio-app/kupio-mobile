package kupio.mobile.core.analytics

class NoOpAnalyticsService : AnalyticsService {
    override fun logEvent(name: String, params: Map<String, String>) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun recordException(throwable: Throwable, context: Map<String, String>) = Unit
    override fun log(message: String) = Unit
}
