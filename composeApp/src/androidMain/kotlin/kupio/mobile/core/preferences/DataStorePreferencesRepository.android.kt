package kupio.mobile.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.mp.KoinPlatform

actual fun createPlatformPreferencesDataStore(): DataStore<Preferences> {
    val context = KoinPlatform.getKoin().get<Context>()
    return createPreferencesDataStore(
        producePath = {
            context.filesDir.resolve(KupioPreferencesFileName).absolutePath
        },
    )
}
