# Provider Optimization Workflow (`/optimize`)

This workflow executes automatically when the user types `/optimize`, asks to "optimize the provider", "audit the repository", or improve the performance, smoothness, UI, and crash resilience of a CloudStream provider codebase.

---

## State Machine

```
[SCAN_WORKSPACE] ──> [PHASE 1: AUDIT] ──> [PHASE 2: REPORT ISSUES] ──> [PHASE 3: APPLY FIXES] ──> [PHASE 4: VERIFY BUILD]
```

---

## Audit Checklist & Inspection Matrix

The audit scans all provider Kotlin classes, extractors, and `build.gradle.kts` files across 6 key pillars:

### 1. Coroutine & Threading Safety (Smoothness & Anti-Freezing)
| Check | Problem | Fix |
|---|---|---|
| **Deprecated `apmap` / `apmapIndexed`** | Calls `runBlocking`, blocking threads and triggering compile error `DeprecationLevel.ERROR`. | Replace with non-blocking `amap` / `amapIndexed`. |
| **Deprecated `argamap`** | Blocking variadic execution. | Replace with `runAllAsync({ task1() }, { task2() })`. |
| **Heavy Main Thread Parsing** | Parsing large JSON or heavy regex directly on UI threads. | Wrap in `withContext(Dispatchers.IO) { ... }` or use coroutine builders. |
| **Unbounded Concurrency** | Launching hundreds of concurrent requests for seasons/episodes at once, causing OOM or site rate-limiting. | Chunk requests or use `amap` with reasonable batches. |

### 2. Network Efficiency & Anti-Rate-Limiting (Speed & Stability)
| Check | Problem | Fix |
|---|---|---|
| **Missing Request Caching** | Static catalogs or episode lists are re-fetched from scratch on every back-and-forth navigation. | Add `cacheTime = 60` (or `1440` for long-term cache) in `app.get(...)`. |
| **Missing Timeouts** | Network calls hang indefinitely on dead streams or slow hosts. | Add explicit `timeout = 30` in `app.get` / `app.post`. |
| **Concurrent Homepage Throttling** | Cloudflare or site throws HTTP 429 when all `mainPage` rows load in parallel. | Set `override var sequentialMainPage = true` and `override var sequentialMainPageDelay = 500L`. |
| **Bypassing Extraction when URL is Known** | Re-requesting detail page when stream link was already in the card. | Set `override val instantLinkLoading = true` and pass link directly as `data`. |

### 3. UI/UX Polish & Rich Metadata (Visual Appeal & User Experience)
| Check | Problem | Fix |
|---|---|---|
| **Plain Grey Detail Screen** | Missing banner backdrop behind movie/show details. | Set `this.backgroundPosterUrl = coverUrl` or backdrop image. |
| **Missing Infinite Scroll** | Home row stops loading after page 1. | Return `newHomePageResponse(HomePageList(request.name, items), hasNext = true)` in `getMainPage`. |
| **Missing Quick Search** | Search suggestions in CloudStream are slow or disabled. | Set `override val hasQuickSearch = true` and `override suspend fun quickSearch(query: String) = search(query)`. |
| **Missing Actors / Trailers / Tags** | Sparse detail view lacking rich info. | Add `addActors(listOf(...))`, `addTrailer(trailerUrl)`, `this.tags = listOf(...)`, and `this.duration = minutes`. |
| **Missing Dub/Sub Status in Anime** | Anime shows do not show whether Dub or Sub is available. | Call `addDubStatus(dubExist = ..., dubEpisodes = ..., subExist = ..., subEpisodes = ...)` on `newAnimeSearchResponse`. |

### 4. Stream Extraction & Multi-Quality Playback
| Check | Problem | Fix |
|---|---|---|
| **Positional ExtractorLink Arguments** | Passing `referer` or `quality` as constructor parameters causes compile errors. | Move `referer`, `quality`, and `headers` inside the trailing `{ }` builder lambda. |
| **Single Quality Bottleneck** | Hardcoding only 1080p, causing buffering for users on slower cellular connections. | Parse multiple source mirrors (1080p, 720p, 480p) and set `this.quality = getQualityFromName(qualityStr)`. |
| **Unresolved Relative URLs** | `href` or image URLs missing domain (`/watch?v=...` or `//cdn...`). | Wrap all extracted URLs in `fixUrl(url)` or `fixUrlNull(url)`. |
| **Stream CDN Header Dropping** | Player fails with 403 Forbidden because video chunks need cookies/referer. | Override `getVideoInterceptor(extractorLink): Interceptor` to attach headers during ExoPlayer playback. |
| **Unpacked Eval Scripts** | Manually decoding `eval(function(p,a,c,k,e,d)...)` with clumsy regex. | Use built-in `JsUnpacker(packedScript).unpack()`. |

### 5. Defensive Parsing & Crash Prevention (Zero NPEs)
| Check | Problem | Fix |
|---|---|---|
| **Hard Null Assertions (`!!`)** | Site changes a CSS class, leading to immediate crash. | Replace `element!!.text()` with safe call `element?.text()?.trim()`. |
| **Fragile String Conversions** | `text.toInt()` throws `NumberFormatException` when site adds commas, letters, or leaves blank. | Use `text.toIntOrNull()` or regex `Regex("\\d+").find(text)?.value?.toIntOrNull()`. |
| **Crashing on Minor JSON Errors** | `app.get(...).parsed<T>()` fails entire `loadLinks` if one server response changes. | Use `parsedSafe<T>()` or `tryParseJson<T>()`. |
| **Over-throwing Exceptions** | Throwing `ErrorLoadingException` on non-critical fields (e.g. plot or year). | Only throw for required fields (like title); make optional fields nullable. |

### 6. Gradle & Manifest Best Practices
| Check | Problem | Fix |
|---|---|---|
| **Missing / Wrong Language** | Plugin does not appear in CloudStream extension list. | Set `language = "en"` (or valid IETF code) in `build.gradle.kts` matching `lang` in `MainAPI`. |
| **Invalid `apiVersion`** | Build fails with unknown property error. | Remove `apiVersion` from `cloudstream { }` block. |
| **JVM Target Incompatibility** | Method not found or bytecode mismatch on older Android versions. | Set JVM target to `1.8` with `-Xno-call-assertions` compiler options. |
| **Jackson Version Drift** | Newer Jackson builds fail on Android 8/9. | Pin `jackson-module-kotlin` to `2.13.1` (or compatible project pin). |

---

## Execution Steps

When `/optimize` is called:

1. **Discover Modules**: Find all folders with `build.gradle.kts` and scan their `.kt` source files.
2. **Execute Full Audit**: Run the 6 inspection matrices against all providers and extractors in the workspace.
3. **Generate Audit Report**: Present a clear table of detected issues (High / Medium / Low severity) with exact file links and line numbers.
4. **Propose Automated Fixes**: Provide clean diffs/code updates for identified issues.
5. **Compile & Verify**: Run `./gradlew <module>:make` to verify that optimizations build cleanly without regressions.
