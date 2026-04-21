package kupio.mobile.features.auth.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider

private val DeviceIdKey = stringPreferencesKey("device_id")

class DataStoreDeviceIdProvider(
    private val dataStore: DataStore<Preferences>,
) : DeviceIdProvider {
    override suspend fun getOrCreate(): String {
        val existingValue = dataStore.data.first()[DeviceIdKey]
        if (existingValue != null) return existingValue

        val generatedValue = randomUuid()
        val updatedPreferences = dataStore.edit { preferences ->
            if (preferences[DeviceIdKey] == null) {
                preferences[DeviceIdKey] = generatedValue
            }
        }

        return updatedPreferences[DeviceIdKey] ?: generatedValue
    }
}
