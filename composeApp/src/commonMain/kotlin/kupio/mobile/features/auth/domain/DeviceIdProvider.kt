package kupio.mobile.features.auth.domain

interface DeviceIdProvider {
    suspend fun getOrCreate(): String
}
