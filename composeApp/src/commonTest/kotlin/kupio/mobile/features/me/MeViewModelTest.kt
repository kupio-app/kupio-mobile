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

    @Test
    fun `theme toggle switches light to dark`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.LIGHT)
        val viewModel = MeViewModel(preferencesRepository = preferences)

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }

    @Test
    fun `theme toggle switches dark to light`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.DARK)
        val viewModel = MeViewModel(preferencesRepository = preferences)

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, preferences.themeMode.value)
    }

    @Test
    fun `theme toggle switches system to dark`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialMode = ThemeMode.SYSTEM)
        val viewModel = MeViewModel(preferencesRepository = preferences)

        viewModel.onIntent(MeIntent.ThemeToggleClicked)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }

    @Test
    fun `open settings emits navigation effect`() = runTest(dispatcher) {
        val viewModel = MeViewModel(
            preferencesRepository = FakePreferencesRepository(initialMode = ThemeMode.SYSTEM),
        )

        viewModel.onIntent(MeIntent.OpenSettingsClicked)
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
    }
}

