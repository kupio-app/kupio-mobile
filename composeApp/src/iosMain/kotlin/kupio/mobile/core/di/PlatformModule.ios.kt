package kupio.mobile.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.liftric.kvault.KVault
import kotlinx.cinterop.ExperimentalForeignApi
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.preferences.KupioPreferencesFileName
import kupio.mobile.core.preferences.createPreferencesDataStore
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual val platformModule = module {
    single<BackendConfig> {
        object : BackendConfig {
            override val baseUrl: String = requireNotNull(
                NSBundle.mainBundle.objectForInfoDictionaryKey("KupioBackendBaseUrl") as? String,
            ) {
                "KupioBackendBaseUrl must be configured in Info.plist."
            }
        }
    }
    single { KVault("kupio.mobile.secure_store") }
    single<DataStore<Preferences>> {
        createPreferencesDataStore(
            producePath = {
                val applicationSupportDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSApplicationSupportDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                )
                val basePath = requireNotNull(
                    requireNotNull(applicationSupportDirectory) {
                        "Application Support directory is unavailable."
                    }.path,
                ) {
                    "Application Support path is unavailable."
                }
                "$basePath/$KupioPreferencesFileName"
            },
        )
    }
}
