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

---

### Step 3 — 2026-10-23: Upstream CloudStream Core Architecture Integration
- **Target / Source Repositories**: `upstream` (`recloudstream` core Android app & library codebase)
- **Category**: Ground-Truth Core Architecture, Built-in Engines, Lifecycle & De-obfuscation
- **Learned Discoveries & Capabilities**:
  1. **Built-in Extractor Engine (80+ Hosts)**: The core app already ships with over 80 verified extractors (`StreamWish`, `VidHidePro`, `Filemoon`, `DoodStream`, `Voe`, `StreamTape`, `MixDrop`, `Gofile`, `HubCloud`). Providers should delegate directly via `loadExtractor(url, subtitleCallback, callback)` rather than reimplementing.
  2. **Native JavaScript Evaluation (`JsInterpreter` & `JsHunter`)**: Discovered the built-in `JsInterpreter.evalJs(code, variable)` native AST parser that runs JavaScript directly in Kotlin without a WebView, and `JsHunter(script).dehunt()` for reversing `eval(function(h,u,n,t,e,r))` obfuscation.
  3. **Master HLS Playlist Parser (`M3u8Helper.generateM3u8`)**: Built-in utility to automatically fetch master `.m3u8` manifests, parse all bitrate streams, and emit multi-quality `ExtractorLink`s (1080p, 720p, 480p, 360p) with zero custom regex.
  4. **Headless Android WebView & Turnstile Solver (`WebViewResolver`)**: Built-in OkHttp interceptor supporting regex URL halting (`interceptUrl`), secondary URL sniffing (`additionalUrls`), and Cloudflare Turnstile bypassing with `useOkhttp = false`.
  5. **Dynamic Global Extractor Registration**: Plugins can expose their custom `ExtractorApi` globally to the entire CloudStream app via `BasePlugin.registerExtractorAPI(extractor)`.
  6. **Plugin Lifecycle & Manifest Loading**: Documented `PathClassLoader` dynamic loading, `manifest.json` requirements, and `beforeUnload()` cleanup hooks.
- **Harvested Files**:
  - [`references/core-architecture.md`](references/core-architecture.md)
  - Updated [`references/provider-cookbook.md`](references/provider-cookbook.md) (Sections 14-17)
- **Impact**: Eliminates redundant custom extractors by prioritizing CloudStream's 80+ built-ins, enables native JS evaluation without webviews, and adds official `WebViewResolver` and `M3u8Helper` patterns.

---

### Step 4 — 2026-10-23: Reverse-Engineered AnimeKai (`animekai.be`) Architecture & MegaPlay Video Streams
- **Target / Source Repositories**: Target site `https://animekai.be/home` & `megaplay.buzz` stream host
- **Category**: Site Recon, Dynamic CDP Reverse Engineering, REST API Discovery & Streaming Pipeline
- **Learned Discoveries & Capabilities**:
  1. **Dual-Path Search Architecture**: Discovered the high-speed REST endpoint `GET /api/search/suggest?q={query}` returning direct JSON containing `title`, `title_jp`, `slug`, `type`, `rating`, `has_sub`, `has_dub`, `sub_count`, `dub_count`, `ep_count` for sub-100ms searches without HTML parsing.
  2. **AJAX Tabbed Home Updates**: Discovered `GET /ajax/home/items?tab={tab}&page={page}` supporting `all-updates`, `sub-updates`, `dub-updates`, and `china-updates` with paginated HTML snippets.
  3. **Direct Sources Routing API**: Discovered `GET /watch/{slug}/ep/{num}/sources` returning JSON with separated `sub` and `dub` server lists pointing directly to `https://megaplay.buzz/stream/mal/{mal_id}/{ep_num}/{sub|dub}`.
  4. **Initial-DOM Complete Episode Layout**: Verified that all episodes (even 1,000+ episode series like *One Piece*) are present in `.eplist ul.range li a` in the initial static DOM with `num`, `data-sub`, and `data-dub` attributes.
  5. **MegaPlay Streaming & Obfuscation**: Reverse-engineered `megaplay.buzz`, identified the required `Referer: https://animekai.be/` header, the internal `getSources?id={player_id}&platform=OTHER` endpoint, WebVTT subtitle tracks, and implemented a resilient `WebViewResolver` fallback to guarantee multi-quality HLS (1080p, 720p, 360p) playback without manual key cracking.
- **Harvested Files**:
  - [`d:\Poject\CloudStream\animekai_context.md`](d:\Poject\CloudStream\animekai_context.md)
  - [`references/archetypes/animekai-architecture.md`](references/archetypes/animekai-architecture.md)
- **Impact**: Provides a complete, tested blueprint for generating a production-ready, zero-maintenance AnimeKai CloudStream provider.

