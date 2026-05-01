package kupio.mobile.core.di

import io.ktor.client.HttpClient
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.network.createKupioHttpClient
import kupio.mobile.core.preferences.DataStorePreferencesRepository
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.data.local.DataStoreDeviceIdProvider
import kupio.mobile.features.auth.data.local.KVaultSecureSessionStore
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.repository.AuthRepositoryImpl
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.data.repository.AuthClock
import kupio.mobile.features.auth.data.repository.SystemAuthClock
import kupio.mobile.features.auth.data.repository.TokenRefreshingAuthenticatedApiClient
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.auth.domain.validation.AuthValidator
import kupio.mobile.features.auth.presentation.auth.AuthViewModel
import kupio.mobile.features.auth.presentation.username.UsernameViewModel
import kupio.mobile.features.chats.data.ChatWebSocket
import kupio.mobile.features.chats.data.ConversationsStore
import kupio.mobile.features.chats.data.remote.ChatApi
import kupio.mobile.features.chats.data.remote.UserApi
import kupio.mobile.features.chats.data.repository.ChatsRepositoryImpl
import kupio.mobile.features.chats.data.repository.MessagesRepositoryImpl
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.chats.domain.repository.ConversationsRefresher
import kupio.mobile.features.chats.domain.repository.MessagesRepository
import kupio.mobile.features.chats.presentation.list.ChatsListViewModel
import kupio.mobile.features.chats.presentation.thread.ChatThreadViewModel
import kupio.mobile.features.listings.presentation.create.CreateViewModel
import kupio.mobile.features.listings.data.remote.CategoriesApi
import kupio.mobile.features.listings.data.remote.ListingsApi
import kupio.mobile.features.listings.data.repository.CategoriesRepositoryImpl
import kupio.mobile.features.listings.data.repository.ListingsRepositoryImpl
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.detail.ListingDetailViewModel
import kupio.mobile.features.listings.presentation.edit.EditListingViewModel
import kupio.mobile.features.listings.presentation.feed.FeedViewModel
import kupio.mobile.features.reports.data.remote.ReportsApi
import kupio.mobile.features.reports.data.repository.ReportsRepositoryImpl
import kupio.mobile.features.reports.domain.repository.ReportsRepository
import kupio.mobile.features.reports.presentation.create.CreateReportViewModel
import kupio.mobile.features.reports.presentation.moderator.ModeratorReportsDashboardViewModel
import kupio.mobile.features.me.data.remote.MeApi
import kupio.mobile.features.me.data.repository.MeRepositoryImpl
import kupio.mobile.features.me.domain.repository.MeRepository
import kupio.mobile.features.me.presentation.mylistings.MyListingsViewModel
import kupio.mobile.features.me.presentation.profile.MeViewModel
import kupio.mobile.features.settings.SettingsViewModel
import kupio.mobile.core.navigation.RootNavigationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val kupioAppModules: List<Module> = listOf(
    platformModule,
    notificationModule,
    module {
        single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<HttpClient> {
            val config = get<BackendConfig>()
            createKupioHttpClient(
                baseUrl = config.baseUrl,
                isDebug = config.isDebug,
            )
        }
        single { AuthApi(get()) }
        single { ListingsApi(get()) }
        single<ListingsRepository> { ListingsRepositoryImpl(get(), get()) }
        single { CategoriesApi(get()) }
        single<CategoriesRepository> { CategoriesRepositoryImpl(get()) }
        single { ChatApi(get()) }
        single { UserApi(get()) }
        single { MeApi(get()) }
        single<MeRepository> { MeRepositoryImpl(get(), get()) }
        single { ReportsApi(get()) }
        single<ReportsRepository> { ReportsRepositoryImpl(get(), get()) }
        single<ChatsRepository> { ChatsRepositoryImpl(get(), get()) }
        single { ChatWebSocket(get(), get(), get(), get()) }
        single<MessagesRepository> { MessagesRepositoryImpl(get(), get(), get(), get()) }
        single { SessionCleaner(get(), get()) }
        single { ConversationsStore(get(), get(), get(), get(), get(), get()) }
        single<ConversationsRefresher> { get<ConversationsStore>() }
        viewModelOf(::ChatsListViewModel)
        viewModel { params -> ChatThreadViewModel(params.get(), get(), get()) }
        single<AuthClock> { SystemAuthClock() }
        single { AuthTokenProvider(get(), get(), get(), get()) }
        single<AuthenticatedApiClient> {
            val koin = getKoin()
            TokenRefreshingAuthenticatedApiClient(
                authTokenProvider = get(),
                onSessionExpired = { koin.get<AuthSessionManager>().expireSession() },
            )
        }
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
        single<SecureSessionStore> { KVaultSecureSessionStore(get()) }
        single<DeviceIdProvider> { DataStoreDeviceIdProvider(get()) }
        single { AuthValidator() }
        single { AuthSessionManager(get(), get()) }
        single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
        viewModelOf(::RootNavigationViewModel)
        viewModelOf(::AuthViewModel)
        viewModelOf(::FeedViewModel)
        viewModelOf(::CreateViewModel)
        viewModelOf(::MeViewModel)
        viewModelOf(::MyListingsViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::UsernameViewModel)
        viewModelOf(::ModeratorReportsDashboardViewModel)
        viewModel { params -> ListingDetailViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { params -> EditListingViewModel(params.get(), get(), get()) }
        viewModel { params -> CreateReportViewModel(params.get(), params.get(), params.get(), params.get(), get()) }
    },
)
