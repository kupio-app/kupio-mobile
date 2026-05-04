package kupio.mobile.features.me

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.CachedAuthenticatedUserStore
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats
import kupio.mobile.features.me.domain.repository.MeRepository
import kupio.mobile.features.me.presentation.mylistings.MyListingsIntent
import kupio.mobile.features.me.presentation.mylistings.MyListingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MyListingsViewModelTest {
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
    fun `toggle click opens confirmation with expected target status`() = runTest(dispatcher) {
        val repository = FakeMeRepository(
            listings = mutableListOf(
                listing(id = "active", status = OwnedListingStatus.ACTIVE),
            ),
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(MyListingsIntent.ToggleActiveClicked("active"))

        val confirmation = viewModel.state.value.statusChangeConfirmation
        assertNotNull(confirmation)
        assertEquals("active", confirmation.listingId)
        assertEquals(OwnedListingStatus.INACTIVE, confirmation.targetStatus)
    }

    @Test
    fun `confirm status change updates listing status and counters`() = runTest(dispatcher) {
        val repository = FakeMeRepository(
            listings = mutableListOf(
                listing(id = "listing-1", status = OwnedListingStatus.ACTIVE),
                listing(id = "listing-2", status = OwnedListingStatus.INACTIVE),
            ),
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(MyListingsIntent.ToggleActiveClicked("listing-1"))
        viewModel.onIntent(MyListingsIntent.ConfirmStatusChange)
        advanceUntilIdle()

        assertEquals("listing-1", repository.lastUpdatedListingId)
        assertEquals(OwnedListingStatus.INACTIVE, repository.lastUpdatedStatus)
        assertEquals(0, viewModel.state.value.activeCount)
        assertEquals(2, viewModel.state.value.listings.count { it.status == OwnedListingStatus.INACTIVE })
        assertEquals(OwnedListingStatus.INACTIVE, viewModel.state.value.listings.first { it.id == "listing-1" }.status)
    }

    private fun buildViewModel(repository: FakeMeRepository): MyListingsViewModel {
        val sessionManager = AuthSessionManager(
            FakeAuthRepository(),
            FakeSecureSessionStore(),
            FakeCachedAuthenticatedUserStore(),
        )
        return MyListingsViewModel(
            meRepository = repository,
            sessionManager = sessionManager,
        )
    }

    private class FakeMeRepository(
        private val listings: MutableList<OwnedListing>,
    ) : MeRepository {
        var lastUpdatedListingId: String? = null
        var lastUpdatedStatus: OwnedListingStatus? = null

        override suspend fun getStats(): UserListingStats =
            UserListingStats(
                activeCount = listings.count { it.status == OwnedListingStatus.ACTIVE },
                inactiveCount = listings.count { it.status == OwnedListingStatus.INACTIVE },
                promotedCount = listings.count { it.isPromoted },
                chatsCount = listings.sumOf { it.chatsCount },
                favouritesCount = listings.sumOf { it.favouritesCount },
            )

        override suspend fun getMyListings(): List<OwnedListing> = listings.toList()

        override suspend fun getMyListing(listingId: String): OwnedListing? =
            listings.firstOrNull { it.id == listingId }

        override suspend fun updateListingStatus(listingId: String, status: OwnedListingStatus) {
            val index = listings.indexOfFirst { it.id == listingId }
            if (index >= 0) {
                listings[index] = listings[index].copy(status = status)
            }
            lastUpdatedListingId = listingId
            lastUpdatedStatus = status
        }
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

    private class FakeCachedAuthenticatedUserStore : CachedAuthenticatedUserStore {
        override suspend fun read(): AuthenticatedUser? = null
        override suspend fun write(user: AuthenticatedUser) = Unit
        override suspend fun clear() = Unit
    }
}

private fun listing(id: String, status: OwnedListingStatus) = OwnedListing(
    id = id,
    title = "Listing $id",
    price = 100,
    currency = Currency.USD,
    primaryImageUrl = null,
    status = status,
    seenCount = 0,
    favouritesCount = 0,
    chatsCount = 0,
    isPromoted = false,
    promotionExpiresAt = null,
)
