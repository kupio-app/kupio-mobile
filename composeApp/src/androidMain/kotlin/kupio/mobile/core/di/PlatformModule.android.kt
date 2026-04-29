package kupio.mobile.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.liftric.kvault.KVault
import kupio.mobile.BuildConfig
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.platform.AndroidPhoneDialer
import kupio.mobile.core.platform.PhoneDialer
import kupio.mobile.core.preferences.KupioPreferencesFileName
import kupio.mobile.core.preferences.createPreferencesDataStore
import org.koin.dsl.module

actual val platformModule = module {
    single<BackendConfig> {
        object : BackendConfig {
            override val baseUrl: String = BuildConfig.KUPIO_BACKEND_BASE_URL
            override val isDebug: Boolean = BuildConfig.DEBUG
        }
    }
    single { KVault(get<Context>(), "kupio.mobile.secure_store") }
    single<PhoneDialer> { AndroidPhoneDialer(get()) }
    single<DataStore<Preferences>> {
        val context: Context = get()
        createPreferencesDataStore(
            producePath = {
                context.filesDir.resolve(KupioPreferencesFileName).absolutePath
            },
        )
    }
}
