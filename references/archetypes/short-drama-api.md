# Short-Drama / OTT Mobile API Archetype

This archetype is designed for platforms serving vertical short dramas, web series, and mobile-first streaming services (e.g. NetShort, DramaBox, ShortMax, ToonBox).

---

## Architectural Characteristics

1. **Pure JSON REST APIs**: Rather than scraping messy HTML, these services communicate via encrypted or token-authenticated mobile endpoints.
2. **Device Authentication**: Initial handshake generates guest tokens, device fingerprints (`deviceId`), and timestamps.
3. **Episode Pagination / Batch Playlists**: Dramas consist of 30–100 micro-episodes (1–2 minutes each) fetched in paginated chunks (e.g. 10 episodes per batch).
4. **VOD CDN Integration**: Video playback URLs are served from signed cloud CDNs (ByteDance VolcEngine, AWS CloudFront, Cloudflare Stream).

---

## Implementation Template

```kotlin
class ShortDramaProvider : MainAPI() {
    override var mainUrl = "https://api.example.com"
    override var name = "ShortDrama"
    override val supportedTypes = setOf(TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    private var token: String? = null

    // 1. Authenticate / Handshake
    private suspend fun getAuthToken(): String {
        token?.let { return it }
        val authRes = app.post(
            "$mainUrl/v1/auth/guest",
            json = mapOf("deviceId" to "cloudstream_${System.currentTimeMillis()}")
        ).parsedSafe<AuthResponse>()
        token = authRes?.data?.token
        return token ?: ""
    }

    // 2. Main Page Feed
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val auth = getAuthToken()
        val res = app.get(
            "$mainUrl/v1/dramas/trending?page=$page&limit=20",
            headers = mapOf("Authorization" to "Bearer $auth")
        ).parsedSafe<DramaFeedResponse>()

        val items = res?.data?.list?.map { drama ->
            newTvSeriesSearchResponse(drama.title, "$mainUrl/drama/${drama.id}", TvType.TvSeries) {
                this.posterUrl = drama.coverUrl
            }
        } ?: emptyList()

        return newHomePageResponse(request.name, items, hasNext = (items.size == 20))
    }

    // 3. Load Title Details & Episodes
    override suspend fun load(url: String): LoadResponse {
        val dramaId = url.substringAfterLast("/")
        val auth = getAuthToken()
        val details = app.get("$mainUrl/v1/dramas/$dramaId", headers = mapOf("Authorization" to "Bearer $auth"))
            .parsedSafe<DramaDetailResponse>()?.data ?: throw ErrorLoadingException("Failed to load details")

        // Fetch episodes in batches
        val episodesRes = app.get("$mainUrl/v1/dramas/$dramaId/episodes", headers = mapOf("Authorization" to "Bearer $auth"))
            .parsedSafe<EpisodeListResponse>()

        val episodes = episodesRes?.data?.episodes?.mapIndexed { index, ep ->
            newEpisode(ep.playUrl ?: ep.id) {
                this.name = "Episode ${index + 1}"
                this.episode = index + 1
            }
        } ?: emptyList()

        return newTvSeriesLoadResponse(details.title, url, TvType.TvSeries, episodes) {
            this.posterUrl = details.coverUrl
            this.plot = details.synopsis
        }
    }

    // 4. Resolve Stream Links
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Direct stream or delegate to VolcEngine/CDN extractor
        if (data.startsWith("http")) {
            callback.invoke(
                newExtractorLink(name, name, data, ExtractorLinkType.VIDEO) {
                    this.quality = Qualities.P1080.value
                }
            )
            return true
        }
        return false
    }
}
```
