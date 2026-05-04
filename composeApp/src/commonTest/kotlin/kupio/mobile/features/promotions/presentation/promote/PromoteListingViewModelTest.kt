package kupio.mobile.features.promotions.presentation.promote

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.UserRole
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImage
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.promotions.domain.model.ListingPromotion
import kupio.mobile.features.promotions.domain.model.PromotionPacket
import kupio.mobile.features.promotions.domain.model.PromotionStatus
import kupio.mobile.features.promotions.domain.model.PromotionType
import kupio.mobile.features.promotions.domain.repository.PromotionsRepository
import kupio.mobile.features.search.domain.model.SearchFilters

@OptIn(ExperimentalCoroutinesApi::class)
class PromoteListingViewModelTest {
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
    fun `load fetches listing preview and active packets`() = runTest(dispatcher) {
        val promotions = FakePromotionsRepository(
            packets = listOf(
                packet(id = 1, active = true),
                packet(id = 2, active = false),
            ),
        )
        val viewModel = createViewModel(promotions = promotions)

        advanceUntilIdle()

        assertEquals("listing-1", viewModel.state.value.listing?.id)
        assertEquals(listOf(1), viewModel.state.value.packets.map { it.id })
        assertEquals(1, viewModel.state.value.selectedPacketId)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `selecting packet and promoting calls repository`() = runTest(dispatcher) {
        val promotions = FakePromotionsRepository(
            packets = listOf(
                packet(id = 1),
                packet(id = 3, type = PromotionType.VIP),
            ),
        )
        val viewModel = createViewModel(promotions = promotions)
        advanceUntilIdle()

        viewModel.onIntent(PromoteListingIntent.PacketSelected(3))
        viewModel.onIntent(PromoteListingIntent.PromoteClicked)
        advanceUntilIdle()

        assertEquals("listing-1" to 3, promotions.promoted.single())
        assertEquals(3, viewModel.state.value.createdPromotion?.packet?.id)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `failed promote keeps selected packet and exposes error`() = runTest(dispatcher) {
        val promotions = FakePromotionsRepository(
            packets = listOf(packet(id = 5)),
            promoteFailure = IllegalStateException("Promotion failed"),
        )
        val viewModel = createViewModel(promotions = promotions)
        advanceUntilIdle()

        viewModel.onIntent(PromoteListingIntent.PromoteClicked)
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.selectedPacketId)
        assertEquals("Promotion failed", viewModel.state.value.submitError)
        assertTrue(viewModel.state.value.createdPromotion == null)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    private fun createViewModel(
        listings: ListingsRepository = FakeListingsRepository(),
        promotions: FakePromotionsRepository = FakePromotionsRepository(),
    ): PromoteListingViewModel {
        return PromoteListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            promotionsRepository = promotions,
            sessionManager = AuthSessionManager(FakeAuthRepository(), FakeSecureSessionStore()),
        )
    }

    private class FakePromotionsRepository(
        private val packets: List<PromotionPacket> = listOf(packet(id = 1)),
        private val promoteFailure: Throwable? = null,
    ) : PromotionsRepository {
        val promoted = mutableListOf<Pair<String, Int>>()

        override suspend fun getPackets(): List<PromotionPacket> = packets

        override suspend fun promoteListing(
            listingId: String,
            packetId: Int,
        ): ListingPromotion {
            promoteFailure?.let { throw it }
            promoted += listingId to packetId
            return ListingPromotion(
                id = "promotion-$packetId",
                listingId = listingId,
                packet = packets.first { it.id == packetId },
                transactionId = null,
                startsAt = "2026-05-04T10:00:00Z",
                expiresAt = "2026-05-11T10:00:00Z",
                status = PromotionStatus.ACTIVE,
                createdAt = "2026-05-04T10:00:00Z",
            )
        }
    }

    private class FakeListingsRepository : ListingsRepository {
        override suspend fun searchListings(
            filters: SearchFilters,
            cursor: String?,
            limit: Int,
        ): ListingFeed = error("unused")

        override suspend fun getFeed(
            limit: Int,
            cursor: String?,
            query: String?,
            categoryId: Int?,
        ): ListingFeed = error("unused")

        override suspend fun getListing(id: String): Listing = listing(id)

        override suspend fun getListingDetail(id: String): Listing = error("unused")

        override suspend fun createListing(listing: CreateListing): Listing = error("unused")

        override suspend fun updateListing(
            listingId: String,
            listing: CreateListing,
            phone: String?,
            contactName: String?,
            isCallsDisabled: Boolean,
        ): Listing = error("unused")

        override suspend fun updateListingStatus(
            listingId: String,
            status: ListingStatus,
        ): Listing = error("unused")

        override suspend fun uploadListingImages(
            listingId: String,
            images: List<ListingImageUpload>,
        ): List<ListingImage> = error("unused")

        override suspend fun deleteListingImage(
            listingId: String,
            imageId: String,
        ) = error("unused")

        override suspend fun updateListingImagesOrder(
            listingId: String,
            imageIds: List<String>,
        ) = error("unused")
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(email: String, password: String): AuthSession = error("unused")
        override suspend fun register(email: String, password: String, username: String): AuthSession = error("unused")
        override suspend fun loginWithGoogle(idToken: String): AuthSession = error("unused")
        override suspend fun refreshSession(): AuthSession = error("unused")
        override suspend fun getCurrentUser(): AuthenticatedUser = AuthenticatedUser(
            id = "user-1",
            username = "user",
            displayName = null,
            email = "user@example.com",
            role = UserRole.USER,
            needsUsername = false,
            balance = 0,
            avatarUrl = null,
            createdAt = "2026-05-04T10:00:00Z",
        )
        override suspend fun setUsername(username: String): AuthenticatedUser = error("unused")
        override suspend fun logout(refreshToken: String) = Unit
    }

    private class FakeSecureSessionStore : SecureSessionStore {
        override suspend fun readSession(): AuthSession? = null
        override suspend fun writeSession(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }
}

private fun packet(
    id: Int,
    type: PromotionType = PromotionType.TOP,
    active: Boolean = true,
) = PromotionPacket(
    id = id,
    name = "Packet $id",
    description = "Promotion packet $id",
    type = type,
    durationDays = 7,
    price = 1000,
    isActive = active,
)

private fun listing(id: String) = Listing(
    id = id,
    title = "Coffee grinder",
    description = "Good condition",
    price = 100,
    currency = Currency.EUR,
    status = ListingStatus.ACTIVE,
    primaryImageUrl = null,
    images = emptyList(),
    imageUrls = emptyList(),
    createdAt = "2026-05-04T10:00:00Z",
    userId = "user-1",
    categoryId = 1,
    categoryName = "Kitchen",
    seenCount = 3,
    phone = null,
    contactName = null,
    isCallsDisabled = false,
    isFree = false,
    isTradable = false,
    customFilters = emptyMap(),
)
