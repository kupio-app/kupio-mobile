package kupio.mobile.features.listings.presentation.detail

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.platform.PhoneDialer
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.chats.domain.repository.ConversationsRefresher
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.ListingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ListingDetailViewModelTest {
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
    fun `load uses detail endpoint and listing contact info`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = createViewModel(listings = listings)

        advanceUntilIdle()

        assertEquals(1, listings.getListingDetailCalls)
        assertEquals("listing-1", listings.detailIds.single())
        assertEquals("Elena K.", viewModel.state.value.seller?.displayName)
        assertEquals("+421900111222", viewModel.state.value.seller?.phone)
    }

    @Test
    fun `send message starts conversation and opens chat`() = runTest(dispatcher) {
        val chats = FakeChatsRepository()
        val refresher = FakeConversationsRefresher()
        val viewModel = createViewModel(chats = chats, conversationsRefresher = refresher)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.OpenMessageDialog)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("Hi, is this still available?"))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertEquals("listing-1" to "Hi, is this still available?", chats.startedConversations.single())
        assertEquals(1, refresher.refreshCalls)
        assertFalse(viewModel.state.value.isMessageDialogVisible)
        assertEquals(ListingDetailEffect.OpenChat("conversation-1"), viewModel.effects.first())
    }

    @Test
    fun `blank message is not sent`() = runTest(dispatcher) {
        val chats = FakeChatsRepository()
        val viewModel = createViewModel(chats = chats)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.OpenMessageDialog)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("   "))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertTrue(chats.startedConversations.isEmpty())
        assertEquals("Enter a message.", viewModel.state.value.messageError)
    }

    @Test
    fun `call action opens dialer only when phone exists`() = runTest(dispatcher) {
        val dialer = FakePhoneDialer()
        val viewModel = createViewModel(phoneDialer = dialer)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.CallSeller)

        assertEquals(listOf("+421900111222"), dialer.openedPhones)
    }

    @Test
    fun `call action ignores missing phone`() = runTest(dispatcher) {
        val dialer = FakePhoneDialer()
        val listings = FakeListingsRepository().apply { phone = null }
        val viewModel = createViewModel(listings = listings, phoneDialer = dialer)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.CallSeller)

        assertTrue(dialer.openedPhones.isEmpty())
    }

    @Test
    fun `call action ignores disabled calls`() = runTest(dispatcher) {
        val dialer = FakePhoneDialer()
        val listings = FakeListingsRepository().apply { isCallsDisabled = true }
        val viewModel = createViewModel(listings = listings, phoneDialer = dialer)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.CallSeller)

        assertTrue(dialer.openedPhones.isEmpty())
    }

    private fun createViewModel(
        listings: FakeListingsRepository = FakeListingsRepository(),
        chats: FakeChatsRepository = FakeChatsRepository(),
        conversationsRefresher: FakeConversationsRefresher = FakeConversationsRefresher(),
        phoneDialer: FakePhoneDialer = FakePhoneDialer(),
    ): ListingDetailViewModel = ListingDetailViewModel(
        listingId = "listing-1",
        listingsRepository = listings,
        chatsRepository = chats,
        conversationsRefresher = conversationsRefresher,
        phoneDialer = phoneDialer,
    )

    private class FakeListingsRepository : ListingsRepository {
        var getListingDetailCalls = 0
        var phone: String? = "+421900111222"
        var isCallsDisabled = false
        val detailIds = mutableListOf<String>()

        override suspend fun getFeed(
            limit: Int,
            cursor: String?,
            query: String?,
            categoryId: Int?,
        ): ListingFeed = ListingFeed(emptyList(), null)

        override suspend fun getListing(id: String): Listing = listing(id, phone, isCallsDisabled)

        override suspend fun getListingDetail(id: String): Listing {
            getListingDetailCalls += 1
            detailIds += id
            return listing(id, phone, isCallsDisabled)
        }

        override suspend fun createListing(listing: CreateListing): Listing =
            listing("created", phone, isCallsDisabled)

        override suspend fun updateListingStatus(listingId: String, status: ListingStatus): Listing =
            listing(listingId, phone, isCallsDisabled)

        override suspend fun uploadListingImages(listingId: String, images: List<ListingImageUpload>) = Unit
    }

    private class FakeChatsRepository : ChatsRepository {
        val startedConversations = mutableListOf<Pair<String, String>>()

        override suspend fun listConversations(role: ChatRole, limit: Int): List<ConversationData> =
            emptyList()

        override suspend fun startConversation(listingId: String, message: String): ConversationData {
            startedConversations += listingId to message
            return ConversationData(
                id = "conversation-1",
                listingId = listingId,
                buyerId = "buyer",
                sellerId = "seller-1",
                createdAt = "2026-04-29T12:00:00Z",
                lastMessagePreview = message,
                unreadCount = 0,
            )
        }

        override suspend fun getUnreadCount(): Int = 0
    }

    private class FakeConversationsRefresher : ConversationsRefresher {
        var refreshCalls = 0
        override suspend fun refresh() {
            refreshCalls += 1
        }
    }

    private class FakePhoneDialer : PhoneDialer {
        val openedPhones = mutableListOf<String>()
        override fun openDialer(phone: String): Boolean {
            openedPhones += phone
            return true
        }
    }

    private companion object {
        fun listing(
            id: String,
            phone: String? = "+421900111222",
            isCallsDisabled: Boolean = false,
        ): Listing = Listing(
            id = id,
            title = "Vintage oak desk",
            description = "Solid oak writing desk in good condition with small signs of normal use.",
            price = 180,
            currency = Currency.EUR,
            primaryImageUrl = "https://example.test/listing.jpg",
            imageUrls = listOf("https://example.test/listing.jpg"),
            createdAt = "2026-04-26T00:00:00Z",
            userId = "seller-1",
            categoryId = 1,
            categoryName = "Furniture",
            seenCount = 12,
            phone = phone,
            contactName = "Elena K.",
            isCallsDisabled = isCallsDisabled,
            customFilters = mapOf("condition" to "Good"),
        )
    }
}
