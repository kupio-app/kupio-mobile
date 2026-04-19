package kupio.mobile.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kupio.mobile.core.preferences.KupioPreferencesFileName
import kupio.mobile.core.preferences.createPreferencesDataStore
import org.koin.dsl.module

actual val platformModule = module {
    single<DataStore<Preferences>> {
        val context: Context = get()
        createPreferencesDataStore(
            producePath = {
                context.filesDir.resolve(KupioPreferencesFileName).absolutePath
            },
        )
    }
}
