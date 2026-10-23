# Live Sports & Events Archetype

This archetype is designed for sports streaming providers (Cricket, Football, UFC, NBA, F1) that aggregate live events, match schedules, countdown states, and AES-encrypted server configurations (e.g. Cricify, Sportzx).

---

## Architectural Characteristics

1. **Time-Based Event Filtering**: Matches have dynamic states (`LIVE`, `UPCOMING`, `FINISHED`). Upcoming matches show start times, while finished matches are hidden or moved to highlights.
2. **Encrypted Configuration Decryption**: Server URLs or stream links are often encrypted with AES-CBC/PKCS5Padding (Hex Key + IV) stored in remote JSON or Firebase configurations.
3. **Multi-Source Failover**: Each match typically provides 3 to 10 mirror servers (English, Hindi, Bengali, Spanish, 1080p 50fps, 720p).
4. **Referer & Origin Player Headers**: CDN streams require specific `Origin` and `Referer` headers passed to ExoPlayer.

---

## Implementation Template

```kotlin
class LiveSportsProvider : MainAPI() {
    override var mainUrl = "https://api.livesports.example"
    override var name = "LiveSports"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    // AES Decryption Helper for Encrypted Server Payloads
    object SportsCrypto {
        fun decryptAesCbc(encryptedBase64: String, hexKey: String, hexIv: String): String? {
            return try {
                val key = hexKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val iv = hexIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                val decrypted = cipher.doFinal(base64DecodeArray(encryptedBase64.trim()))
                String(decrypted, Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val matches = app.get("$mainUrl/api/matches").parsedSafe<MatchResponse>()?.matches ?: emptyList()

        val liveList = matches.filter { it.isLive }.map { match ->
            newLiveSearchResponse("${match.teamA} vs ${match.teamB}", match.id, TvType.Live) {
                this.posterUrl = match.bannerUrl
            }
        }

        return newHomePageResponse(
            listOf(
                HomePageList("🔴 Live Matches Now", liveList, isHorizontalImages = true)
            )
        )
    }

    override suspend fun load(url: String): LoadResponse {
        val matchId = url.substringAfterLast("/")
        val match = app.get("$mainUrl/api/matches/$matchId").parsedSafe<MatchDetail>()
            ?: throw ErrorLoadingException("Match unavailable")

        return newLiveStreamLoadResponse("${match.teamA} vs ${match.teamB}", url, match.id) {
            this.posterUrl = match.bannerUrl
            this.plot = "Tournament: ${match.tournament} | Status: ${match.status}"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val servers = app.get("$mainUrl/api/matches/$data/servers").parsedSafe<List<ServerInfo>>() ?: emptyList()

        servers.amap { server ->
            // If encrypted, decrypt using SportsCrypto
            val streamUrl = if (server.isEncrypted && server.key != null && server.iv != null) {
                SportsCrypto.decryptAesCbc(server.streamUrl, server.key, server.iv) ?: server.streamUrl
            } else {
                server.streamUrl
            }

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "$name - ${server.name} (${server.language})",
                    url = streamUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.quality = Qualities.P1080.value
                    this.referer = server.referer ?: mainUrl
                }
            )
        }
        return true
    }
}
```
