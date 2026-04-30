package kupio.mobile.features.listings.presentation.detail

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.platform.PhoneDialer
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.UserRole
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.MessageSender
import kupio.mobile.features.chats.domain.model.WsMessageEvent
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.chats.domain.repository.ConversationsRefresher
import kupio.mobile.features.chats.domain.repository.MessagesRepository
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImage
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats
import kupio.mobile.features.me.domain.repository.MeRepository

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

        viewModel.onIntent(ListingDetailIntent.OpenMessageSheet)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("Hi, is this still available?"))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertEquals("listing-1" to "Hi, is this still available?", chats.startedConversations.single())
        assertEquals(1, refresher.refreshCalls)
        assertFalse(viewModel.state.value.isMessageSheetVisible)
        assertEquals(ListingDetailEffect.OpenChat("conversation-1"), viewModel.effects.first())
    }

    @Test
    fun `send message reuses existing conversation for listing and seller`() = runTest(dispatcher) {
        val chats = FakeChatsRepository(
            conversations = listOf(
                conversation(id = "existing-conversation", listingId = "listing-1", sellerId = "seller-1"),
            ),
        )
        val messages = FakeMessagesRepository()
        val refresher = FakeConversationsRefresher()
        val viewModel = createViewModel(
            chats = chats,
            messages = messages,
            conversationsRefresher = refresher,
        )
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.OpenMessageSheet)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("Can I pick it up today?"))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertTrue(chats.startedConversations.isEmpty())
        assertEquals("existing-conversation" to "Can I pick it up today?", messages.sentMessages.single())
        assertEquals(1, refresher.refreshCalls)
        assertEquals(ListingDetailEffect.OpenChat("existing-conversation"), viewModel.effects.first())
    }

    @Test
    fun `blank message is not sent`() = runTest(dispatcher) {
        val chats = FakeChatsRepository()
        val viewModel = createViewModel(chats = chats)
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.OpenMessageSheet)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("   "))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertTrue(chats.startedConversations.isEmpty())
        assertEquals("Enter a message.", viewModel.state.value.messageError)
    }

    @Test
    fun `load marks own listing from current user id`() = runTest(dispatcher) {
        val viewModel = createViewModel(currentUserId = "seller-1")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isOwnListing)
    }

    @Test
    fun `own listing loads owner metadata from me listings`() = runTest(dispatcher) {
        val me = FakeMeRepository(
            ownerListing = ownedListing(
                id = "listing-1",
                status = OwnedListingStatus.INACTIVE,
                seenCount = 24,
                favouritesCount = 8,
                chatsCount = 3,
                isPromoted = true,
            ),
        )
        val viewModel = createViewModel(me = me, currentUserId = "seller-1")
        advanceUntilIdle()

        val ownerMetadata = viewModel.state.value.ownerMetadata
        assertEquals(listOf("listing-1"), me.ownerListingRequests)
        assertEquals(ListingStatus.INACTIVE, ownerMetadata?.status)
        assertEquals(24, ownerMetadata?.seenCount)
        assertEquals(8, ownerMetadata?.favouritesCount)
        assertEquals(3, ownerMetadata?.chatsCount)
        assertEquals(true, ownerMetadata?.isPromoted)
    }

    @Test
    fun `own listing does not start conversation`() = runTest(dispatcher) {
        val chats = FakeChatsRepository()
        val viewModel = createViewModel(chats = chats, currentUserId = "seller-1")
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.OpenMessageSheet)
        viewModel.onIntent(ListingDetailIntent.MessageChanged("Hello"))
        viewModel.onIntent(ListingDetailIntent.SendMessage)
        advanceUntilIdle()

        assertTrue(chats.startedConversations.isEmpty())
    }

    @Test
    fun `own listing status toggle deactivates active listing`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = createViewModel(listings = listings, currentUserId = "seller-1")
        advanceUntilIdle()

        viewModel.onIntent(ListingDetailIntent.ToggleOwnerStatus)
        advanceUntilIdle()

        assertEquals(ListingStatus.INACTIVE, viewModel.state.value.statusChangeTarget)
        viewModel.onIntent(ListingDetailIntent.ConfirmOwnerStatusChange)
        advanceUntilIdle()

        assertEquals("listing-1" to ListingStatus.INACTIVE, listings.statusUpdates.single())
        assertEquals(ListingStatus.INACTIVE, viewModel.state.value.listing?.status)
        assertEquals(ListingStatus.INACTIVE, viewModel.state.value.ownerMetadata?.status)
        assertEquals(null, viewModel.state.value.statusChangeTarget)
        assertFalse(viewModel.state.value.isUpdatingStatus)
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

    private suspend fun createViewModel(
        listings: FakeListingsRepository = FakeListingsRepository(),
        me: FakeMeRepository = FakeMeRepository(),
        chats: FakeChatsRepository = FakeChatsRepository(),
        messages: FakeMessagesRepository = FakeMessagesRepository(),
        conversationsRefresher: FakeConversationsRefresher = FakeConversationsRefresher(),
        phoneDialer: FakePhoneDialer = FakePhoneDialer(),
        currentUserId: String = "buyer-1",
    ): ListingDetailViewModel {
        val sessionManager = AuthSessionManager(
            authRepository = FakeAuthRepository(currentUserId),
            secureSessionStore = FakeSecureSessionStore(),
        )
        if (currentUserId.isNotBlank()) {
            sessionManager.establishSession(
                AuthSession(
                    accessToken = "access",
                    refreshToken = "refresh",
                    accessExpiresAt = Long.MAX_VALUE,
                    refreshExpiresAt = Long.MAX_VALUE,
                ),
            )
        }
        return ListingDetailViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            meRepository = me,
            chatsRepository = chats,
            messagesRepository = messages,
            conversationsRefresher = conversationsRefresher,
            phoneDialer = phoneDialer,
            sessionManager = sessionManager,
        )
    }

    private class FakeListingsRepository : ListingsRepository {
        var getListingDetailCalls = 0
        var phone: String? = "+421900111222"
        var isCallsDisabled = false
        val detailIds = mutableListOf<String>()
        val statusUpdates = mutableListOf<Pair<String, ListingStatus>>()

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

        override suspend fun updateListing(
            listingId: String,
            listing: CreateListing,
            phone: String?,
            contactName: String?,
            isCallsDisabled: Boolean,
        ): Listing = listing(listingId, phone, isCallsDisabled)

        override suspend fun updateListingStatus(listingId: String, status: ListingStatus): Listing {
            statusUpdates += listingId to status
            return listing(listingId, phone, isCallsDisabled, status)
        }

        override suspend fun uploadListingImages(
            listingId: String,
            images: List<ListingImageUpload>,
        ): List<ListingImage> = emptyList()

        override suspend fun deleteListingImage(listingId: String, imageId: String) = Unit

        override suspend fun updateListingImagesOrder(listingId: String, imageIds: List<String>) = Unit
    }

    private class FakeMeRepository(
        private val ownerListing: OwnedListing? = ownedListing(id = "listing-1"),
    ) : MeRepository {
        val ownerListingRequests = mutableListOf<String>()

        override suspend fun getStats(): UserListingStats = UserListingStats(
            activeCount = 0,
            inactiveCount = 0,
            promotedCount = 0,
            chatsCount = 0,
            favouritesCount = 0,
        )

        override suspend fun getMyListings(): List<OwnedListing> =
            ownerListing?.let(::listOf).orEmpty()

        override suspend fun getMyListing(listingId: String): OwnedListing? {
            ownerListingRequests += listingId
            return ownerListing?.takeIf { it.id == listingId }
        }

        override suspend fun updateListingStatus(listingId: String, status: OwnedListingStatus) = Unit
    }

    private class FakeChatsRepository(
        private val conversations: List<ConversationData> = emptyList(),
    ) : ChatsRepository {
        val startedConversations = mutableListOf<Pair<String, String>>()

        override suspend fun listConversations(role: ChatRole, limit: Int): List<ConversationData> =
            conversations

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

    private class FakeMessagesRepository : MessagesRepository {
        val sentMessages = mutableListOf<Pair<String, String>>()

        override suspend fun loadMessages(conversationId: String): List<MessageItem> = emptyList()

        override suspend fun sendMessage(conversationId: String, body: String): MessageItem {
            sentMessages += conversationId to body
            return MessageItem(
                id = "message-1",
                sender = MessageSender.ME,
                text = body,
                timeLabel = "12:00",
                createdAtIso = "2026-04-29T12:00:00Z",
            )
        }

        override suspend fun sendTyping(conversationId: String) = Unit

        override fun observeMessages(conversationId: String): Flow<WsMessageEvent> = emptyFlow()
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

    private class FakeAuthRepository(
        private val currentUserId: String,
    ) : AuthRepository {
        override suspend fun login(email: String, password: String): AuthSession = error("Unused")
        override suspend fun register(email: String, password: String, username: String): AuthSession = error("Unused")
        override suspend fun loginWithGoogle(idToken: String): AuthSession = error("Unused")
        override suspend fun refreshSession(): AuthSession = error("Unused")
        override suspend fun getCurrentUser(): AuthenticatedUser = AuthenticatedUser(
            id = currentUserId,
            username = "current",
            displayName = "Current User",
            email = "current@example.test",
            role = UserRole.USER,
            needsUsername = false,
            balance = 0,
            avatarUrl = null,
        )
        override suspend fun setUsername(username: String): AuthenticatedUser = error("Unused")
        override suspend fun logout(refreshToken: String) = Unit
    }

    private class FakeSecureSessionStore : SecureSessionStore {
        private var session: AuthSession? = null
        override suspend fun readSession(): AuthSession? = session
        override suspend fun writeSession(session: AuthSession) {
            this.session = session
        }
        override suspend fun clear() {
            session = null
        }
    }

    private companion object {
        fun conversation(
            id: String,
            listingId: String,
            sellerId: String,
        ): ConversationData = ConversationData(
            id = id,
            listingId = listingId,
            buyerId = "buyer-1",
            sellerId = sellerId,
            createdAt = "2026-04-29T12:00:00Z",
            lastMessagePreview = null,
            unreadCount = 0,
        )

        fun listing(
            id: String,
            phone: String? = "+421900111222",
            isCallsDisabled: Boolean = false,
            status: ListingStatus = ListingStatus.ACTIVE,
        ): Listing = Listing(
            id = id,
            title = "Vintage oak desk",
            description = "Solid oak writing desk in good condition with small signs of normal use.",
            price = 180,
            currency = Currency.EUR,
            status = status,
            primaryImageUrl = "https://example.test/listing.jpg",
            images = listOf(
                ListingImage(
                    id = "image-1",
                    url = "https://example.test/listing.jpg",
                    sortOrder = 0,
                ),
            ),
            imageUrls = listOf("https://example.test/listing.jpg"),
            createdAt = "2026-04-26T00:00:00Z",
            userId = "seller-1",
            categoryId = 1,
            categoryName = "Furniture",
            seenCount = 12,
            phone = phone,
            contactName = "Elena K.",
            isCallsDisabled = isCallsDisabled,
            isFree = false,
            isTradable = false,
            customFilters = mapOf("condition" to "Good"),
        )

        fun ownedListing(
            id: String,
            status: OwnedListingStatus = OwnedListingStatus.ACTIVE,
            seenCount: Int = 12,
            favouritesCount: Int = 4,
            chatsCount: Int = 2,
            isPromoted: Boolean = true,
        ): OwnedListing = OwnedListing(
            id = id,
            title = "Vintage oak desk",
            price = 180,
            currency = Currency.EUR,
            primaryImageUrl = "https://example.test/listing.jpg",
            status = status,
            seenCount = seenCount,
            favouritesCount = favouritesCount,
            chatsCount = chatsCount,
            isPromoted = isPromoted,
            promotionExpiresAt = "2026-05-01T00:00:00Z",
        )
    }
}
