# Add Provider Workflow (`/provider <url>`)

This workflow executes automatically when the user types `/provider <url>`, gives a website URL with `/provider`, or asks to create/add a new CloudStream provider for a specific website.

---

## State Machine

```
[RECEIVE_URL] ──> [PHASE 1: RECON] ──> [PHASE 2: ARCHITECTURE] ──> [PHASE 3: IMPLEMENT] ──> [PHASE 4: VERIFY]
```

---

## Phase 1 — Target Site Recon & Stream Feasibility

**Goal:** Understand the target website's HTML layout, API endpoints, and confirm playable video stream extraction before writing provider code.

### Step 1.1: Fetch Homepage & Base Metadata
- Use `read_url_content` or `browser_subagent` to fetch the root URL.
- Note:
  - Base URL (e.g. `https://example.to`)
  - Site branding / Name
  - Primary language (e.g. `en`, `hi`, `fr`)
  - Content type (`TvType.Movie`, `TvType.TvSeries`, `TvType.Anime`, `TvType.Live`)

### Step 1.2: Identify Search Endpoint
- Check common patterns or form actions in HTML:
  - `GET /search?q={query}` or `GET /?s={query}`
  - `POST /search` with form data `{"search": "query"}`
  - AJAX/JSON endpoints: `/ajax/search?q=...` or `/api/v1/search?kw=...`
- Test the search endpoint with a popular sample title (e.g., "Avatar" or "Batman").
- Identify:
  - CSS selector for search item cards (e.g., `div.flw-item`, `div.movie-card`, `article.item`)
  - Title selector & attribute (`a.title`, `img[alt]`)
  - Detail page link selector (`a[href]`)
  - Poster image URL (`img[src]`, `img[data-src]`)

### Step 1.3: Identify Homepage / Catalog Rows
- Check homepage sections for category carousels/rows:
  - "Trending", "Latest Movies", "New TV Episodes", "Top Rated"
- Map out the `mainPageOf(...)` list:
  ```kotlin
  override val mainPage = mainPageOf(
      "$mainUrl/trending" to "Trending",
      "$mainUrl/movies" to "Recent Movies",
      "$mainUrl/tv-shows" to "TV Shows"
  )
  ```
- Identify pagination query parameter (`?page=2`, `/page/2/`, or AJAX scrolling).

### Step 1.4: Inspect Sample Detail Page (Metadata & Episodes)
- Fetch a movie page and a TV show page (if site supports both).
- Locate metadata selectors:
  - Title: `h1.heading-name`, `h1.title`
  - Poster: `div.film-poster img[src]`
  - Plot / Synopsis: `div.description`, `div.overview`
  - Year: `span.year`, `.imdb-rating`
- Check Episode structure for TV Series:
  - Are episodes on the same page, or loaded via separate AJAX endpoints (e.g., `/ajax/season/episodes?id=...`)?
  - Does the site have seasons? Record season IDs and episode lists.

### Step 1.5: Locate Video Player & Stream Links (Critical Feasibility Check)
- Inspect the player area on an episode or movie page:
  - **Iframe embed:** Look for `<iframe src="https://embed-host.com/e/...">`.
  - **Server list:** Look for `<div class="servers">` with `data-id`, `data-embed`, or `data-src`.
  - **API stream call:** Check network tab or JS scripts for encrypted keys or stream endpoints.
- Check if the embed host is already supported by CloudStream built-in extractors (`StreamTape`, `Vidcloud`, `Superstream`, `Mixdrop`, `Doodstream`, `Filemoon`, `Streamwish`, etc.).
- If custom or obfuscated: inspect how the embed decodes the `.m3u8` or `.mp4` link (Base64, AES, or JS unpacker).

---

## Phase 2 — CloudStream Architecture & Provider Mapping

**Goal:** Plan the provider structure, module name, package, and classes.

### Step 2.1: Determine Naming & Conventions
- **Module Name:** `<SanitizedSiteName>Provider` (PascalCase, e.g., `FlixWaveProvider`, `MovieRulzProvider`)
- **Package Name:** `com.<sitename_lowercase>` (e.g., `com.flixwave`, `com.movierulz`)
- **Provider Class:** `<SanitizedSiteName>Provider : MainAPI()`
- **Plugin Class:** `<SanitizedSiteName>Plugin : BasePlugin()`

### Step 2.2: Map Capabilities
- `supportedTypes = setOf(TvType.Movie, TvType.TvSeries)` (or Anime / Cartoon)
- `hasMainPage = true`
- `lang = "..."` (matching site language, e.g., `"en"`)
- Decide if custom `ExtractorApi` is needed or standard `loadExtractor(...)` call is sufficient.

---

## Phase 3 — Implementation & Module Scaffolding

**Goal:** Write clean, idiomatic Kotlin code adhering to the CloudStream 3 API.

### Step 3.1: Create Directory Structure
```
<SanitizedSiteName>Provider/
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── com/<sitename>/
                ├── <SanitizedSiteName>Provider.kt
                └── <SanitizedSiteName>Plugin.kt
```

### Step 3.2: Create `<SanitizedSiteName>Provider/build.gradle.kts`
```kotlin
version = 1

cloudstream {
    description = "Watch movies and series from <SiteName>"
    authors = listOf("Auto-generated")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=<domain>&sz=%size%"
    language = "en"
}
```

### Step 3.3: Implement `<SanitizedSiteName>Provider.kt`
- Inherit from `MainAPI()`.
- Implement `search(query: String): List<SearchResponse>`
- Implement `getMainPage(page: Int, request: MainPageRequest): HomePageResponse`
- Implement `load(url: String): LoadResponse` (handling both Movie and TV Series if applicable)
- Implement `loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean`
- Add any necessary custom `ExtractorApi` if the video host requires special headers/decryption.
- Use `fixUrl` / `fixUrlNull` for all relative links and image URLs.
- Always use `newExtractorLink(...) { ... }` with trailing lambda for optional parameters (`referer`, `quality`).

### Step 3.4: Implement `<SanitizedSiteName>Plugin.kt`
```kotlin
package com.<sitename>

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class <SanitizedSiteName>Plugin : BasePlugin() {
    override fun load() {
        registerMainAPI(<SanitizedSiteName>Provider())
    }
}
```

---

## Phase 4 — Validation & Verification

### Step 4.1: Syntax & Dependency Sanity Check
- Verify that Jackson imports and usage match version `2.13.1`.
- Verify that `newExtractorLink` does not pass optional arguments positionally.
- Verify `language` in `build.gradle.kts` matches `lang` property in `MainAPI`.

### Step 4.2: Build Check (If in a Gradle CloudStream Project)
- Run `./gradlew <SanitizedSiteName>Provider:make` (or `.\gradlew.bat <SanitizedSiteName>Provider:make`).
- Ensure no compilation or stub errors exist.

### Step 4.3: Present Summary to User
Report:
1. Site analysis findings (search endpoint, detail page selectors, video host embed).
2. Created files and directories.
3. How to build and test locally (`./gradlew <ModuleName>:make` or `deployWithAdb`).
