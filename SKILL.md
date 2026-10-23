---
name: cloudstream-provider-dev
description: >-
  Build, fix, heal, or extend CloudStream (recloudstream) "providers" — Kotlin plugins
  that let the CloudStream Android app search, browse, and stream video from a
  specific website. Features an autonomous self-learning loop that trains itself
  from conversation context, captures reusable extractors, bumps versions, and auto-syncs.
  Use any time the user wants a CloudStream extension/provider/plugin, mentions
  MainAPI, ExtractorApi, TestPlugins, .cs3 files, or needs to fix/heal an existing provider.
triggers:
  - /provider
  - provider
  - add provider
  - new provider
  - /optimize
  - optimize
  - optimize provider
  - audit provider
  - /heal
  - heal
  - heal provider
  - /update-provider
  - update provider
  - fix provider
  - /learn
  - learn
  - cloudstream
  - cloudstream provider
  - cloudstream plugin
  - cloudstream extension
  - MainAPI
  - ExtractorApi
  - TestPlugins
  - .cs3
  - plugins.json
  - repo.json
  - recloudstream
  - streaming provider kotlin
tags:
  - kotlin
  - android
  - cloudstream
  - scraping
  - streaming
  - plugin
  - self-learning
---

# CloudStream Provider Development & Autonomous Self-Learning Skill

CloudStream is an Android streaming app that loads community-written plugins ("providers") at
runtime. A provider is a Kotlin class that knows how to search a specific site, list its home
page, load a title's metadata/episodes, and resolve the actual playable video URL. Compiled
plugins ship as `.cs3` files (a renamed zip containing `classes.dex` + `manifest.json`) referenced
from a `plugins.json` that CloudStream fetches from a repo.

---

## ⚡ Core Slash Commands

### 1. `/provider <url>` (Create New Provider)
Executes the automated 4-phase workflow in **[`workflows/add-provider.md`](workflows/add-provider.md)**:
1. **Phase 1 — Recon & Stream Feasibility**: Test target site DOM, verify playable `.m3u8`/`.mp4` streams return HTTP 200 before writing any code.
2. **Phase 2 — Architecture Mapping**: Match site to known archetypes in `references/archetypes/` and select extractor strategies.
3. **Phase 3 — Implementation**: Scaffold `build.gradle.kts`, `Provider.kt` (`search`, `getMainPage`, `load`, `loadLinks`), and `Plugin.kt`.
4. **Phase 4 — Two-Tier Verification**: Compile via `./gradlew <ProviderName>:make` to generate `.cs3`.
5. **Phase 5 — Self-Learning & Auto-Push**: Execute `rules/self-learning-loop.md` to persist new discoveries and push to GitHub.

### 2. `/heal <ProviderName>` or `/update-provider` (Fix Broken Provider)
Executes the diagnosis and repair workflow in **[`workflows/heal-provider.md`](workflows/heal-provider.md)**:
1. **Diagnose**: Inspect changed site DOM, redirected URLs, or broken stream host embed iframes.
2. **Repair**: Swap with updated modular extractors from `references/extractors/` or reverse-engineer the new embed.
3. **Mandatory Version Increment**: Increment integer `version` in `<Provider>/build.gradle.kts` to trigger in-app updates for all CloudStream users.
4. **Compile & Push**: Verify compilation with `./gradlew <Provider>:make`, commit and push extension repo, and advance skill step.

### 3. `/optimize` (6-Pillar Deep Audit)
Executes the 6-pillar repository audit in **[`workflows/optimize-provider.md`](workflows/optimize-provider.md)**:
- **Coroutines**: Replace blocking `apmap`/`runBlocking` with non-blocking `amap`.
- **Anti-Rate-Limiting**: Add `cacheTime = 60`, `timeout = 30`, and `sequentialMainPage = true`.
- **UI Polish**: Add backdrops, actors, trailers, infinite scrolling, and Anime dub/sub status.
- **Playback**: Add multi-quality builder lambdas and player headers via `getVideoInterceptor`.
- **Zero NPEs**: Safe calls (`?.`), `toIntOrNull()`, and `parsedSafe<T>()`.
- **Gradle Integrity**: Verify JVM 1.8 target, Jackson `2.13.1` pinning, and language tags.

### 4. `/learn` (Trigger Autonomous Step Advancement)
Executes the context-mining and persistence protocol in **[`workflows/learn-step.md`](workflows/learn-step.md)**:
- Scans current turn for new extractors, decryption algorithms, and layout patterns.
- Saves standalone Kotlin extractors to `references/extractors/`.
- Appends Step N entry to [`LEARNING_LOG.md`](LEARNING_LOG.md).
- Increments skill version and executes triple-location sync + GitHub push.

---

## 🧠 Autonomous Continuous Learning Architecture (`rules/self-learning-loop.md`)

This skill is designed to train itself 1 step further every single time you work with it:

```
[TASK COMPLETED] ──► [MINE CONTEXT] ──► [HARVEST EXTRACTORS] ──► [UPDATE LEARNING_LOG] ──► [TRIPLE SYNC & PUSH]
```

### Knowledge Repositories:
- **Modular Extractors Library**: Standalone Kotlin files in [`references/extractors/`](references/extractors/) (`VolcEngineExtractor.kt`, `AWSStreamExtractor.kt`, `UpnsPlayerExtractor.kt`, `GDMirrorExtractor.kt`).
- **Site Archetypes Library**: Blueprints for common platforms in [`references/archetypes/`](references/archetypes/) (`short-drama-api.md`, `anime-multiserver.md`, `bdix-ftp-portal.md`).
- **Master Evolution Ledger**: Complete historical record in [`LEARNING_LOG.md`](LEARNING_LOG.md).

---

## ⚡ Mandatory Rules Always in Effect

1. **Self-Learning Loop**: Advance the skill 1 step, save reusable extractors, update `LEARNING_LOG.md`, and bump version after every task ([`rules/self-learning-loop.md`](rules/self-learning-loop.md)).
2. **Provider Version Bump**: Always increment integer `version` in `<Provider>/build.gradle.kts` on any modification ([`rules/provider-version-bump.md`](rules/provider-version-bump.md)).
3. **Always Push to GitHub**: Commit with semantic conventional messages and push all workspace changes before ending turn ([`rules/auto-push-github.md`](rules/auto-push-github.md)).

---

## Two-Tier Verification Gate

Before claiming any provider or extractor is functional:
1. **Tier 1 — Stream Sniffing**: The video stream URL (`.m3u8` or `.mp4`) must be fetched and return HTTP 200 with media content.
2. **Tier 2 — Gradle Compilation**:
   ```bash
   ./gradlew <ProviderName>:make
   ```
   Must successfully produce `<ProviderName>.cs3` in `build/` without compilation errors.

---

## Reference Documentation (Open as Needed)

- **`references/extractors/`** — Production-ready Kotlin `ExtractorApi` implementations ready for instant inclusion.
- **`references/archetypes/`** — End-to-end architecture patterns for Short Dramas, Anime multi-servers, and BDIX FTP portals.
- **`references/scraping-guide.md`** — Devtools inspection, bypassing bot guards, handling AES/Base64 obfuscation, and header injection.
- **`references/provider-cookbook.md`** — Full recipes for movies, TV series, pagination, and data passing.
- **`references/project-setup.md`** — Gradle configs, CI workflow gotchas, JitPack stubs, and `repo.json`/`plugins.json`.

---

## Quick Reference & API Cheat Sheet

| Task | Pattern |
|---|---|
| HTTP GET | `val doc = app.get(url, headers = mapOf("Referer" to mainUrl)).document` |
| HTTP POST Form | `val res = app.post(url, data = mapOf("query" to q)).parsedSafe<MyResponse>()` |
| Concurrent Async Map | `items.amap { app.get(it.url) }` or `items.amapIndexed { idx, it -> ... }` |
| Unpack Eval JS | `val unpacked = JsUnpacker(scriptText).unpack()` |
| Unshorten URL | `val finalUrl = unshortenLinkSafe(shortUrl)` |
| Fix relative URL | `val fullUrl = fixUrl(element.attr("href"))` / `fixUrlNull(...)` |
| Search Result | `newMovieSearchResponse(name, url, TvType.Movie) { this.posterUrl = poster }` |
| TV Series Result | `newTvSeriesSearchResponse(name, url, TvType.TvSeries) { this.posterUrl = poster }` |
| Episode builder | `newEpisode(dataOrUrl) { this.name = title; this.season = s; this.episode = ep }` |
| ExtractorLink builder | `newExtractorLink(source, name, url, ExtractorLinkType.M3U8) { this.referer = ref; this.quality = Qualities.Unknown.value }` |
| Auto-extract embed | `loadExtractor(embedUrl, referer, subtitleCallback, callback)` |
| Parse JSON | `val data = parseJson<MyDataClass>(jsonString)` or `app.get(url).parsed<MyDataClass>()` |
| Live TV / Event | `newLiveSearchResponse(name, url, TvType.Live)` / `newLiveStreamLoadResponse(name, url, dataUrl)` |
| ClearKey DRM DASH | `newDrmExtractorLink(name, name, mpdUrl, ExtractorLinkType.DASH, CLEARKEY_UUID, "oct", kidB64Url, keyB64Url)` |
| Anime Dub/Sub | `newAnimeSearchResponse(name, url, TvType.Anime) { addDubStatus(dubExist = true, dubEpisodes = d, subExist = true, subEpisodes = s) }` |
| Backdrops & Extras | `this.backgroundPosterUrl = backdrop; addActors(listOf("...")); addTrailer(trailerUrl)` |
| Player Stream Interceptor | Override `getVideoInterceptor(extractorLink): Interceptor` to attach auth/headers during playback |
