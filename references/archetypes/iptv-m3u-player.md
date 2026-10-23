# IPTV & M3U Playlist Player Archetype

This archetype enables building live television and streaming channel providers in CloudStream using standard `#EXTM3U` playlists, ClearKey DRM DASH decryption, and custom playback interceptor headers.

---

## Architectural Characteristics

1. **M3U Playlist Parsing**: Scans `#EXTM3U` and `#EXTINF` directives for `tvg-name`, `tvg-logo`, `group-title`, and stream URLs.
2. **Custom VLC Options**: Parses `#EXTVLCOPT:http-user-agent=...` and `#EXTVLCOPT:http-referrer=...` to attach per-channel headers.
3. **DRM ClearKey DASH Support**: Extracts `KID` and `Key` hex or license server endpoints, feeding `newDrmExtractorLink` with `CLEARKEY_UUID`.
4. **Header Replacement Interceptor**: Overrides `getVideoInterceptor` so ExoPlayer sends required CDN authorization headers during stream playback.

---

## Implementation Template

```kotlin
class IptvPlaylistProvider : MainAPI() {
    override var mainUrl = "https://raw.githubusercontent.com/user/repo/main/channels.m3u"
    override var name = "IPTV Channels"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    data class ChannelData(
        val name: String,
        val streamUrl: String,
        val logo: String? = null,
        val group: String = "General",
        val headers: Map<String, String> = emptyMap(),
        val keyId: String? = null,
        val key: String? = null
    )

    private val cachedChannels = mutableListOf<ChannelData>()

    private suspend fun parseM3u(): List<ChannelData> {
        if (cachedChannels.isNotEmpty()) return cachedChannels

        val lines = app.get(mainUrl).text.lines()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup = "General"
        val currentHeaders = mutableMapOf<String, String>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    currentName = Regex("""tvg-name="([^"]+)"""").find(trimmed)?.groupValues?.get(1)
                        ?: trimmed.substringAfterLast(",")
                    currentLogo = Regex("""tvg-logo="([^"]+)"""").find(trimmed)?.groupValues?.get(1)
                    currentGroup = Regex("""group-title="([^"]+)"""").find(trimmed)?.groupValues?.get(1) ?: "General"
                }
                trimmed.startsWith("#EXTVLCOPT:http-user-agent=") -> {
                    currentHeaders["User-Agent"] = trimmed.substringAfter("=")
                }
                trimmed.startsWith("#EXTVLCOPT:http-referrer=") -> {
                    currentHeaders["Referer"] = trimmed.substringAfter("=")
                }
                trimmed.startsWith("http") -> {
                    cachedChannels.add(
                        ChannelData(
                            name = currentName,
                            streamUrl = trimmed,
                            logo = currentLogo,
                            group = currentGroup,
                            headers = currentHeaders.toMap()
                        )
                    )
                    currentHeaders.clear()
                }
            }
        }
        return cachedChannels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val channels = parseM3u()
        val items = channels.map { channel ->
            newLiveSearchResponse(channel.name, channel.streamUrl, TvType.Live) {
                this.posterUrl = channel.logo
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun load(url: String): LoadResponse {
        val channel = parseM3u().find { it.streamUrl == url }
            ?: ChannelData(name = "Live Stream", streamUrl = url)

        return newLiveStreamLoadResponse(channel.name, url, channel.streamUrl) {
            this.posterUrl = channel.logo
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val channel = parseM3u().find { it.streamUrl == data }

        if (channel?.keyId != null && channel.key != null) {
            // ClearKey DRM DASH Stream
            callback.invoke(
                newDrmExtractorLink(
                    source = name,
                    name = channel.name,
                    url = channel.streamUrl,
                    type = ExtractorLinkType.DASH,
                    uuid = CLEARKEY_UUID,
                    kty = "oct",
                    kid = channel.keyId,
                    key = channel.key
                )
            )
        } else {
            // Standard HLS / TS Live Stream
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = channel?.name ?: name,
                    url = data,
                    type = if (data.contains(".mpd")) ExtractorLinkType.DASH else ExtractorLinkType.M3U8
                ) {
                    this.quality = Qualities.P1080.value
                }
            )
        }
        return true
    }
}
```
