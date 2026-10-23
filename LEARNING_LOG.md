# CloudStream Skill Continuous Learning Master Ledger

This ledger tracks the autonomous evolution of the **CloudStream Provider Development Skill** step-by-step. Each entry documents new video extractors, de-obfuscation patterns, platform archetypes, and bug fixes harvested from active development sessions.

---

### Step 1 — 2026-10-23: Bootstrap Self-Learning Engine & Harvest Core Repositories
- **Target / Source Repositories**: `nehal-CloudStream` (`NetShortProvider`, `AnimeDekhoProvider`, `AnimekaiProvider`, `MovieBoxProvider`, `CastleTvProvider`, `FTPBDMedia`)
- **Category**: Architecture / Self-Learning Bootstrap & Extractor Harvesting
- **Learned Discoveries & Capabilities**:
  1. **Short-Drama & OTT API Architecture**: Decrypted ByteDance VolcEngine VOD streams (`vod.ap-southeast-1.volcengineapi.com`), multi-quality signed streams (1080p, 720p, 480p, 360p), and device fingerprint authentication.
  2. **Anime Multi-Server De-obfuscation**: Solved packed JavaScript player scripts (`eval(p,a,c,k,e,d)`) using `JsUnpacker` to extract hidden M3U8 URLs and WebVTT caption tracks.
  3. **High-Speed BDIX Direct Streaming**: Implemented zero-latency `instantLinkLoading = true` streaming for local ISP and FTP portals without extra extractor overhead.
  4. **Self-Training Architecture**: Created the continuous learning protocol (`rules/self-learning-loop.md`), automated `/heal` maintenance workflow, and two-tier verification gate (HTTP 200 stream test + `./gradlew :make`).
- **Harvested Files**:
  - [`references/extractors/VolcEngineExtractor.kt`](references/extractors/VolcEngineExtractor.kt)
  - [`references/extractors/AWSStreamExtractor.kt`](references/extractors/AWSStreamExtractor.kt)
  - [`references/extractors/UpnsPlayerExtractor.kt`](references/extractors/UpnsPlayerExtractor.kt)
  - [`references/extractors/GDMirrorExtractor.kt`](references/extractors/GDMirrorExtractor.kt)
  - [`references/archetypes/short-drama-api.md`](references/archetypes/short-drama-api.md)
  - [`references/archetypes/anime-multiserver.md`](references/archetypes/anime-multiserver.md)
  - [`references/archetypes/bdix-ftp-portal.md`](references/archetypes/bdix-ftp-portal.md)
- **Impact**: All future provider requests can immediately instantiate VolcEngine, AWSStream, Upns, or GDMirror extractors without reverse-engineering from scratch.

---

### Step 2 — 2026-10-23: Harvest CNCVerse Ecosystem (HubCloud, IPTV M3U, DRM ClearKey, Live Sports & HDrezka De-obfuscation)
- **Target / Source Repositories**: `CNCVerse-Cloud-Stream-Extension` (`MLSBDProvider`, `M3UPlaylistPlayerProvider`, `CricifyProvider`, `HDrezkaProvider`, `BilibiliProvider`)
- **Category**: Extractors, IPTV & Live Sports Archetypes, Advanced De-obfuscation
- **Learned Discoveries & Capabilities**:
  1. **HubCloud Multi-Mirror Resolver**: Reusable extractor resolving BuzzServer (`HX-Redirect`), PixelDrain (`/api/file/{id}?download`), FSL Server, S3, and FSLv2 CDNs.
  2. **IPTV & M3U Playlist Player Archetype**: Full parser for `#EXTM3U`, `#EXTINF`, `#EXTVLCOPT:http-user-agent`, and `#EXTVLCOPT:http-referrer` tags with live category carousels.
  3. **ClearKey DRM DASH Playback**: Support for `newDrmExtractorLink` with `CLEARKEY_UUID`, `oct` key format, and license server kid/k key mapping.
  4. **Live Sports & AES-CBC Decrypted Config**: Sports match event state machines (`LIVE` vs `UPCOMING`), multi-audio mirror feeds, and AES-CBC/PKCS5Padding hex key/iv deciphering.
  5. **Base64 Permutation Junk Stripping**: De-obfuscation algorithm (HDrezka style) that generates combinatorial permutations of junk characters (`@`, `#`, `!`, `^`, `$`), base64 encodes them, and strips them from payload streams before base64 decoding.
- **Harvested Files**:
  - [`references/extractors/HubCloudExtractor.kt`](references/extractors/HubCloudExtractor.kt)
  - [`references/archetypes/iptv-m3u-player.md`](references/archetypes/iptv-m3u-player.md)
  - [`references/archetypes/live-sports-events.md`](references/archetypes/live-sports-events.md)
  - Updated [`references/scraping-guide.md`](references/scraping-guide.md) (Section 8: DRM & Permutation Junk Stripping)
- **Impact**: The agent can now instantly generate production-ready IPTV playlist providers, live sports event aggregators, ClearKey DRM stream links, and HubCloud-protected stream resolvers.
