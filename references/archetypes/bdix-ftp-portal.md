# BDIX / Local ISP FTP Media Portal Archetype

This archetype is designed for high-speed local ISP media portals (Bangladeshi BDIX servers like FTPBD, DiscoveryFTP, DhakaFlix, ICCFTP, CircleFTP, CineplexBD) which serve direct high-bandwidth media streams to subscribers.

---

## Architectural Characteristics

1. **Subnet / IP Gated Access**: Portals may require connection to specific ISP IP ranges (BDIX). If user is off-network, requests return 403 or redirect.
2. **Directory Listing or Lightweight Web UI**: Often powered by simple Apache `mod_autoindex`, NGINX indexing, or basic PHP/Bootstrap portals.
3. **Zero Transcode / Direct Storage**: Video links are direct HTTP file URLs (`.mp4`, `.mkv`) hosted on multi-gigabit local storage nodes.
4. **Instant Link Loading**: Can set `override val instantLinkLoading = true` so CloudStream skips `loadLinks` and starts playback directly from the catalog URL.

---

## Implementation Template

```kotlin
class BdixFtpProvider : MainAPI() {
    override var mainUrl = "http://172.16.50.4" // Or public FTP domain
    override var name = "BDIXMedia"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "bn"
    override val hasMainPage = true

    // Direct high-speed playback without separate extractor hop
    override val instantLinkLoading = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("$mainUrl/movies/page/$page/").document
        val items = doc.select("div.movie-card").mapNotNull { card ->
            val title = card.selectFirst(".title")?.text()?.trim() ?: return@mapNotNull null
            val href = fixUrl(card.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val poster = fixUrlNull(card.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items, hasNext = items.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.movie-title")?.text()?.trim() ?: "Movie"
        val poster = fixUrlNull(doc.selectFirst("div.poster-image img")?.attr("src"))
        val directStreamUrl = fixUrl(doc.selectFirst("a.btn-play, a[href$='.mp4'], a[href$='.mkv']")?.attr("href") ?: url)

        return newMovieLoadResponse(title, url, TvType.Movie, directStreamUrl) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Direct stream playback
        callback.invoke(
            newExtractorLink(
                source = name,
                name = "$name Direct",
                url = data,
                type = if (data.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ) {
                this.quality = Qualities.P1080.value
            }
        )
        return true
    }
}
```
