package kupio.mobile.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.liftric.kvault.KVault
import kotlinx.cinterop.ExperimentalForeignApi
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.platform.IosPhoneDialer
import kupio.mobile.core.platform.PhoneDialer
import kupio.mobile.core.preferences.KupioPreferencesFileName
import kupio.mobile.core.preferences.createPreferencesDataStore
import kupio.mobile.core.notifications.BackgroundSyncScheduler
import kupio.mobile.core.notifications.BackgroundSyncSchedulerImpl
import kupio.mobile.core.offline.db.KupioDatabase
import kupio.mobile.core.offline.db.createKupioDatabase
import kupio.mobile.core.offline.db.getKupioDatabaseBuilder
import kupio.mobile.core.offline.IosOfflineSyncScheduler
import kupio.mobile.core.offline.OfflineSyncScheduler
import kupio.mobile.core.offline.files.IosOfflineFileStore
import kupio.mobile.core.offline.files.OfflineFileStore
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
            override val isDebug: Boolean = when (
                val value = NSBundle.mainBundle.objectForInfoDictionaryKey("KupioDebugEnabled")
            ) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                else -> false
            }
        }
    }
    single<BackgroundSyncScheduler> { BackgroundSyncSchedulerImpl() }
    single<OfflineSyncScheduler> { IosOfflineSyncScheduler(get(), get()) }
    single<KupioDatabase> { createKupioDatabase(getKupioDatabaseBuilder()) }
    single<OfflineFileStore> { IosOfflineFileStore() }
    single { KVault("kupio.mobile.secure_store") }
    single<PhoneDialer> { IosPhoneDialer() }
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
