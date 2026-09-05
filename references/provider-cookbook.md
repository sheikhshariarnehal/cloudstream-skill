# Provider Cookbook: Search, Home Page, Load, LoadLinks

Fuller, closer-to-real-world versions of each of the four core `MainAPI` methods. Adapt these —
don't copy them verbatim, since every site's HTML/JSON shape differs.

Table of contents:
1. Search
2. Home page (`mainPageOf` + `getMainPage`)
3. Load (result page: metadata + episodes)
4. LoadLinks (resolving playable video links)
5. Passing structured data instead of a raw URL
6. Writing a custom extractor

---

## 1. Search

The pattern: fetch the search results page/endpoint, select the repeated "card" element for each
result, and map each one to a `SearchResponse`. Share the card → `SearchResponse` conversion with
the home page function below, since it's usually the same shape.

```kotlin
override suspend fun search(query: String): List<SearchResponse> {
    return app.post(mainUrl, data = mapOf("search" to query))
        .document
        .select("div.card-body")
        .mapNotNull { it.toSearchResponse() }
}

private fun Element.toSearchResponse(): SearchResponse? {
    val link = selectFirst("div.alternative a") ?: return null
    val href = link.attr("href")
    val img = selectFirst("div.thumb img")
    val title = img?.attr("alt")?.removePrefix("Watch ") ?: return null

    return newMovieSearchResponse(title, href, TvType.Movie) {
        posterUrl = fixUrl(img.attr("src"))
    }
}
```

Notes:
- `fixUrl` / `fixUrlNull` are CloudStream helpers that turn relative/protocol-less URLs
  (`/watch?v=...`, `//cdn.example.com/x.jpg`) into absolute ones — always run scraped URLs through
  them.
- There are several `SearchResponse` variants — `MovieSearchResponse`, `TvSeriesSearchResponse`,
  `AnimeSearchResponse`, `LiveSearchResponse` — each with fields specific to that content type
  (e.g. anime's sub/dub status, episode counts). Pick the one matching `TvType`.
- Any optional metadata (e.g. a detected language/region) is fine to add on a best-effort basis;
  it's not required for a working provider.

## 2. Home page

Declare the home-screen rows as `(requestData, displayName)` pairs; CloudStream calls
`getMainPage` once per row (and again with an incremented `page` as the user scrolls), letting
episodes/pages load incrementally instead of all at once.

```kotlin
override val mainPage = mainPageOf(
    "1" to "Recent Release - Sub",
    "2" to "Recent Release - Dub",
)

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val params = mapOf("page" to page.toString(), "type" to request.data)
    val html = app.get("$mainUrl/ajax/page-recent-release.html", params = params).text

    val items = parseRegex.findAll(html).map {
        val (link, epNum, title, poster) = it.destructured
        newAnimeSearchResponse(title, link) {
            posterUrl = poster
            addDubStatus(request.data == "2", epNum.toIntOrNull())
        }
    }.toList()

    return newHomePageResponse(request.name, items)
}
```

`request.data` is whatever you put on the left of the pair above; `request.name` is the row title
shown in the app. `page` starts at 1 and increases as the user scrolls — use it for pagination
params if the endpoint supports them, or ignore it if the source has no real pagination.

## 3. Load (result/detail page)

This is usually the biggest function in a provider, because it has to gather all the metadata for
a nice-looking detail page. The core idea is still "select elements, pull text/attrs out of
them" — it just has more fields:

```kotlin
override suspend fun load(url: String): LoadResponse {
    val doc = app.get(url).document
    val details = doc.select("div.detail_page-watch")
    val img = details.select("img.film-poster-img")

    val title = img.attr("title").ifBlank { null } ?: throw ErrorLoadingException("No title")
    val posterUrl = img.attr("src")
    val plot = details.select("div.description").text().removePrefix("Overview:").trim()
    val year = doc.selectFirst(".fs-item > .imdb")?.text()
        ?.let { Regex("""\d{4}""").find(it)?.value?.toIntOrNull() }

    val isMovie = url.contains("/movie/")
    val id = details.attr("data-id").ifEmpty {
        Regex(""".*-(\d+)""").find(url)?.groupValues?.get(1)
            ?: throw ErrorLoadingException("Unable to get id from '$url'")
    }

    return if (isMovie) {
        // A single "episode" (the movie itself) is enough data for loadLinks to work with later.
        newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
            this.plot = plot
            this.year = year
        }
    } else {
        val episodes = arrayListOf<Episode>()
        val seasons = app.get("$mainUrl/ajax/v2/tv/seasons/$id").document
            .select("div.dropdown-menu > a")

        seasons.apmapIndexed { seasonIndex, seasonEl ->
            val seasonId = seasonEl.attr("data-id").ifBlank { return@apmapIndexed }
            val epDocs = app.get("$mainUrl/ajax/v2/season/episodes/$seasonId").document
                .select("ul > li > a")

            epDocs.forEachIndexed { i, epEl ->
                episodes.add(
                    newEpisode(epEl.attr("data-id")) {
                        this.name = epEl.text()
                        this.season = seasonIndex + 1
                        this.episode = i + 1
                    }
                )
            }
        }

        newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = posterUrl
            this.plot = plot
            this.year = year
        }
    }
}
```

Important details this glosses over on real sites:
- **Episodes are never paginated in CloudStream.** If a series has 20 seasons spread across
  separate pages on the source site, `load` must fetch and flatten every one of them itself —
  there's no "load more episodes" callback later.
- Use `...OrNull` variants (`toIntOrNull`, `selectFirst(...)?.text()`) everywhere plausible —
  a single unexpectedly-missing field shouldn't crash the whole page. Reserve throwing
  `ErrorLoadingException` for genuinely required fields (like the title), since that message is
  shown directly to the user and doubles as a debugging aid.
- The value you put as an episode's "url"/data (here, `epEl.attr("data-id")`) is exactly what
  `loadLinks` receives later — see the JSON-data pattern below for passing more than a bare id.

## 4. LoadLinks

`loadLinks` receives whatever string you stored as the url/data in `load`/episodes, and must call
`callback` once per playable link it finds (and `subtitleCallback` for any subtitle tracks). It
returns `true` if it found at least one link.

Simple case — the source URL directly embeds a host CloudStream (or your own extractor) already
knows how to resolve:

```kotlin
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val doc = app.get(data).document
    var found = false
    doc.select("a[data-embed]").forEach { server ->
        val embedUrl = fixUrl(server.attr("data-embed"))
        // loadExtractor matches embedUrl's domain against CloudStream's built-in extractors
        // and calls the right one automatically.
        found = found or loadExtractor(embedUrl, data, subtitleCallback, callback)
    }
    return found
}
```

Two-step case — the host needs its own request chain before you get a real playlist URL (e.g. an
ID has to be encrypted first, then exchanged for a stream manifest):

```kotlin
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val loadData = parseJson<MyLoadData>(data)

    val encrypted = app.get("https://enc.example/api?id=${loadData.id}")
        .parsed<EncryptResponse>().result ?: return false

    val stream = app.get(
        "https://host.example/api/${loadData.type}/$encrypted",
        headers = mapOf("Referer" to "https://host.example/", "Origin" to "https://host.example")
    ).parsed<StreamResponse>()

    val playlist = stream.playlist ?: return false

    callback(
        newExtractorLink("HostName", "HostName", playlist, ExtractorLinkType.M3U8) {
            this.referer = "https://host.example/"
            this.quality = Qualities.Unknown.value
        }
    )
    return true
}
```

`newExtractorLink(...)` takes `referer`/`quality`/`headers`/etc. through the trailing builder
lambda, **not** as extra constructor arguments — passing them positionally is a common compile
error people hit when copying older examples.

## 5. Passing structured data instead of a raw URL

Instead of storing a bare URL as a `SearchResponse`'s/`Episode`'s "url", you can serialize a small
data class to JSON and store *that* string instead. `load`/`loadLinks` then just
`parseJson<T>(data)` it back out. This is the cleanest way to carry more than one piece of state
(e.g. an external API's id *and* a season/episode number) between the search/home-page stage and
the load/loadLinks stage, without needing extra requests just to re-derive that state later:

```kotlin
data class MyLoadData(val id: String, val type: String, val season: Int? = null, val episode: Int? = null)

// when building a SearchResponse/Episode:
newMovieSearchResponse(title, MyLoadData(id, "movie").toJson(), TvType.Movie) { ... }

// later, in load()/loadLinks():
val data = parseJson<MyLoadData>(url)
```

## 6. Writing a custom extractor

When a video host isn't already supported, isolate its decoding logic in its own `ExtractorApi`
rather than inlining it into `loadLinks` — this keeps `loadLinks` simple and lets the extractor be
reused by other providers that embed the same host:

```kotlin
class MyHostExtractor : ExtractorApi() {
    override val name = "MyHost"
    override val mainUrl = "https://myhost.example"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mediaId = app.get(url).document
            .selectFirst("li[data-id]")?.attr("data-id") ?: return

        val sources = app.get("$mainUrl/ajax/embed/$mediaId/sources")
            .parsedSafe<MyHostSources>() ?: return

        sources.result?.forEach { source ->
            val decoded = decodeSourceUrl(source.encUrl) // your own deobfuscation logic
            callback(
                newExtractorLink(name, name, decoded, ExtractorLinkType.M3U8) {
                    this.referer = referer ?: mainUrl
                    this.quality = Qualities.Unknown.value
                }
            )
        }
    }
}
```

Prefer `parsedSafe<T>()` (returns null on failure) over `parsed<T>()` inside extractors, since a
single host's API hiccup shouldn't crash the whole `loadLinks` call for every other server on the
page.