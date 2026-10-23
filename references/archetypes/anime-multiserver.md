# Anime Multi-Server Aggregator Archetype

This archetype is designed for anime streaming sites that feature multiple servers (Sub/Dub, HD-1, HD-2, StreamWish, VidHide, MegaCloud) and episode seasons with dual audio tracks (e.g. AnimeDekho, Animekai, HiAnime, Aniwatch).

---

## Architectural Characteristics

1. **Sub/Dub Dual Selection**: Episodes or servers are tagged with audio tracks (`Sub` vs `Dub`).
2. **Server List AJAX Endpoints**: Clicking an episode makes an AJAX call to retrieve available embed iframes (`/ajax/episode/servers?id=...`).
3. **Multi-Quality M3U8 Playlist Parsing**: Master playlists contain multiple bitrates (1080p, 720p, 480p, 360p) with embedded WebVTT subtitles.
4. **Extractor Delegation**: Player iframes are delegated directly to CloudStream's `loadExtractor(url, subtitleCallback, callback)`.

---

## Implementation Template

```kotlin
class AnimeAggregatorProvider : MainAPI() {
    override var mainUrl = "https://anime.example.com"
    override var name = "AnimeAggregator"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)
    override var lang = "en"
    override val hasMainPage = true

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.title")?.text()?.trim() ?: "Anime"
        val poster = fixUrlNull(doc.selectFirst("div.poster img")?.attr("src"))
        val plot = doc.selectFirst("div.synopsis")?.text()?.trim()

        // Extract episodes
        val episodes = doc.select("ul.episodes-list li a").mapIndexed { idx, epElement ->
            val epNum = epElement.attr("data-number").toIntOrNull() ?: (idx + 1)
            val epHref = fixUrl(epElement.attr("href"))
            newEpisode(epHref) {
                this.name = epElement.text().trim()
                this.episode = epNum
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = plot
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // Locate all server embed buttons
        val serverElements = doc.select("div.server-item[data-embed], iframe[src]")
        serverElements.amap { server ->
            val embedUrl = fixUrl(server.attr("data-embed").ifEmpty { server.attr("src") })
            // Delegate automatically to registered CloudStream extractors
            loadExtractor(embedUrl, subtitleCallback, callback)
        }

        return true
    }
}
```
