package kupio.mobile.features.auth.domain.repository

interface DeviceIdProvider {
    suspend fun getOrCreate(): String
}
