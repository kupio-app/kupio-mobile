package kupio.mobile.features.home

data class HomeContent(
    val title: String,
    val body: String,
    val note: String,
)

interface HomeContentRepository {
    fun getContent(): HomeContent
}

class InMemoryHomeContentRepository : HomeContentRepository {
    override fun getContent(): HomeContent {
        return HomeContent(
            title = "Kupio Starter",
            body = "This starter defines the shared app shell and demonstrates the initial MVI-lite structure.",
            note = "Koin is active now. DataStore-backed theme persistence is added next.",
        )
    }
}

// TODO: Replace the in-memory starter repository with a real feature-specific data source when home becomes a real screen.
