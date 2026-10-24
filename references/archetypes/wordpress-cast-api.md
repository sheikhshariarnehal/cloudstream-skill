# Archetype: WordPress Private Streaming & REST Cast API (`wordpress-cast-api`)

## Profile & Target Platforms
- **Platform Type**: WordPress CMS paired with custom REST API streaming modules, Plyr/Video.js engines, and Google Cast integration endpoints.
- **Representative Site**: `mojaloss.stream`
- **Key Characteristics**:
  - Content catalog, taxonomies, and search are exposed via WordPress core (`/?s=`, `/category/`, `/page/%d/`) or custom REST endpoints (`/wp-json/mojaloss/v1/search`, `/wp-json/mojaloss/v1/today`).
  - Video stream URLs (`.mp4`, `.m3u8`, `.mpd`) are guarded behind authentication and resolved either dynamically by client player scripts (`script#plyr-tv-scanned`, `window.MojalossCast`, `#movie-video`) or through authenticated REST Cast endpoints (`/wp-json/mojaloss/v1/cast/movie`, `/wp-json/mojaloss/v1/cast/tv`).
  - Anti-bot / WAF protections (Apache mod_security, Wordfence, Cloudflare) protect sensitive endpoints like `wp-login.php` with 403 Forbidden against non-browser User-Agents or requests lacking `Accept`/`Origin`/`Referer` headers.

---

## 1. Authentication Architecture

### A. Session Cookie Verification
WordPress REST API (`/wp-json/wp/v2/users/me`) rejects cookie-only requests with 401/403 unless accompanied by a REST API nonce (`X-WP-Nonce`).
**Best Practice**:
Verify session validity by probing administrative redirect or home page state:
1. `GET /wp-admin/` with `allowRedirects = false`:
   - If authenticated: returns `200 OK` or redirects to dashboard.
   - If unauthenticated: returns `302 Redirect` to `wp-login.php`.
2. Inspect home page HTML for `body.logged-in`, `[data-authed='1']`, or `a[href*='action=logout']`.

### B. Direct Login Flow (`wp-login.php`)
1. Perform initial `GET /wp-login.php` with desktop Chrome `User-Agent` and `Accept: text/html` to acquire `wordpress_test_cookie`.
2. Submit `POST /wp-login.php` with:
   - Headers: `User-Agent`, `Referer`, `Origin`, `Accept`, `Accept-Language`, `Cookie: wordpress_test_cookie=WP%20Cookie%20check`.
   - Form Data: `log`, `pwd`, `rememberme=forever`, `redirect_to=/wp-admin/`, `testcookie=1`.
3. Capture resulting `wordpress_logged_in_*` and `wordpress_sec_*` cookies from both `Set-Cookie` response headers and client cookie jar.

---

## 2. Stream Resolution Pipeline

### Step 1 — Page Scraping & CSRF Nonce Extraction
Before querying the REST Cast endpoints, fetch the target title's HTML page:
```kotlin
val pageRes = app.get(mediaData.url, headers = headers, timeout = 30L)
val doc = pageRes.document
val html = pageRes.text

// Extract WP REST Nonce required for cookie-authenticated REST calls
val wpNonce = Regex(""""nonce"\s*:\s*"([a-zA-Z0-9]+)"""").find(html)?.groupValues?.getOrNull(1)
```

### Step 2 — Cast REST API Resolution
Send a JSON POST to `/wp-json/mojaloss/v1/cast/movie` or `/wp-json/mojaloss/v1/cast/tv`:
```json
{
  "post_id": "251004",
  "season": 1,
  "episode": 1,
  "quality": "default"
}
```
**Required Headers**:
- `Content-Type: application/json`
- `X-WP-Nonce: <extracted_nonce>`
- `Cookie: <wordpress_logged_in_cookie>`
- `Referer: https://<domain>/`
- `Origin: https://<domain>`

**Expected Response**:
```json
{
  "media_url": "https://media.domain/path/movie.mp4",
  "content_type": "video/mp4",
  "title": "Movie Title",
  "tracks": [
    {
      "content_id": "https://media.domain/subs/en.vtt",
      "language": "en",
      "name": "English"
    }
  ]
}
```

### Step 3 — Plyr / DOM Player Fallback
If the Cast API is disabled or inaccessible:
1. Parse `script#plyr-tv-scanned` / `script#plyr-config` JSON:
   - Extract `baseUrl`, `showFolder`, `seasons[].episode_files`, and `seasons[].dash_manifests`.
   - Construct progressive MP4 or DASH manifest stream URLs with URL encoding.
2. Inspect DOM video elements (`#movie-video`, `#plyr-video`, `video`):
   - Check `data-default-src`, `data-4k-src`, and `src`.
   - Extract subtitle tracks from `<track src="..." srclang="...">`.

---

## 3. Playback Video Interceptor
Streams delivered from private WordPress media clusters require session cookies and valid referer headers during ExoPlayer playback:
```kotlin
override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
    return Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Referer", "$mainUrl/")
            .header("Origin", mainUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
        val cookie = MojaLossStorage.getCookie()
        if (!cookie.isNullOrEmpty()) {
            builder.header("Cookie", cookie)
        }
        chain.proceed(builder.build())
    }
}
```
