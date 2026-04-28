package kupio.mobile.features.me

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats
import kupio.mobile.features.me.domain.repository.MeRepository
import kupio.mobile.features.me.presentation.profile.MeEffect
import kupio.mobile.features.me.presentation.profile.MeIntent
import kupio.mobile.features.me.presentation.profile.MeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(preferences: PreferencesRepository): MeViewModel {
        val sessionManager = AuthSessionManager(FakeAuthRepository(), FakeSecureSessionStore())
        return MeViewModel(
            preferencesRepository = preferences,
            sessionManager = sessionManager,
            meRepository = FakeMeRepository(),
        )
    }

    @Test
    fun `theme toggle switches light to dark`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.LIGHT)
        val viewModel = buildViewModel(preferences)
        advanceUntilIdle()

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }

    @Test
    fun `theme toggle switches dark to light`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.DARK)
        val viewModel = buildViewModel(preferences)
        advanceUntilIdle()

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, preferences.themeMode.value)
    }

    @Test
    fun `theme toggle switches system to dark`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.SYSTEM)
        val viewModel = buildViewModel(preferences)
        advanceUntilIdle()

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }

    @Test
    fun `settings clicked emits navigation effect`() = runTest(dispatcher) {
        val viewModel = buildViewModel(FakePreferencesRepository(initialMode = ThemeMode.SYSTEM))

        viewModel.onIntent(MeIntent.SettingsClicked)
        val effect = viewModel.effects.first()

        assertEquals(MeEffect.NavigateToSettings, effect)
    }

    private class FakePreferencesRepository(
        initialMode: ThemeMode,
    ) : PreferencesRepository {
        override val themeMode = MutableStateFlow(initialMode)

        override suspend fun setThemeMode(mode: ThemeMode) {
            themeMode.value = mode
        }

        override fun chatLastSeenEpochMillis(conversationId: String) = MutableStateFlow<Long?>(null)

        override suspend fun markChatSeen(conversationId: String, epochMillis: Long) = Unit
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(email: String, password: String): AuthSession = error("unused")
        override suspend fun register(email: String, password: String, username: String): AuthSession = error("unused")
        override suspend fun loginWithGoogle(idToken: String): AuthSession = error("unused")
        override suspend fun refreshSession(): AuthSession = error("unused")
        override suspend fun getCurrentUser(): AuthenticatedUser = error("unused")
        override suspend fun setUsername(username: String): AuthenticatedUser = error("unused")
        override suspend fun logout(refreshToken: String) = Unit
    }

    private class FakeSecureSessionStore : SecureSessionStore {
        override suspend fun readSession(): AuthSession? = null
        override suspend fun writeSession(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeMeRepository : MeRepository {
        override suspend fun getStats(): UserListingStats =
            UserListingStats(activeCount = 0, inactiveCount = 0, promotedCount = 0, chatsCount = 0, favouritesCount = 0)

        override suspend fun getMyListings(): List<OwnedListing> = emptyList()

        override suspend fun updateListingStatus(listingId: String, status: OwnedListingStatus) = Unit
    }
}
