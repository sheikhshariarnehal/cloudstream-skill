# CloudStream Upstream Core Architecture & Internals

This guide documents the ground-truth internal architecture of the CloudStream Android app (`recloudstream`/`upstream`), directly mined from `com.lagradost.cloudstream3`.

---

## 🏛️ 1. Plugin Lifecycle & Loading Mechanism

CloudStream loads compiled plugins (`.cs3` files) dynamically using Android's `dalvik.system.PathClassLoader`:

```
[ .cs3 ZIP ] ──► [ manifest.json ] ──► [ PathClassLoader ] ──► [ BasePlugin.load() ] ──► [ APIHolder ]
```

### Manifest Schema (`manifest.json` inside `.cs3`)
```json
{
  "name": "MySiteProvider",
  "pluginClassName": "com.mysite.MySitePlugin",
  "requiresResources": false,
  "version": 1
}
```

### Plugin Registration Hooks (`BasePlugin`)
Every plugin entry point extends `BasePlugin` and is annotated with `@CloudstreamPlugin`:
```kotlin
package com.mysite

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MySitePlugin : BasePlugin() {
    override fun load() {
        // 1. Register MainAPI provider (Search, Catalog, Load, Links)
        registerMainAPI(MySiteProvider())

        // 2. Register custom standalone ExtractorApi globally in the app
        registerExtractorAPI(MyCustomHosterExtractor())
    }

    override fun beforeUnload() {
        // Cleanup resources, cancel background coroutines or clear caches
    }
}
```

> [!TIP]
> Calling `registerExtractorAPI(MyExtractor())` registers your custom extractor with CloudStream's global extractor registry. Any other provider in the app can now automatically resolve streams from your host via `loadExtractor(url, ...)`.

---

## ⚡ 2. Built-in Extractor Engine & `loadExtractor`

CloudStream's core library contains over **80 built-in extractors**:
- **Common Stream Hosts**: `StreamWish`, `VidHide` / `VidHidePro`, `Filemoon`, `DoodStream`, `Voe`, `StreamTape`, `StreamSB`, `StreamRuby`, `MixDrop`, `Gofile`, `Krakenfiles`, `Luluvdo`, `XStreamCdn`, `VkExtractor`, `HubCloud`.

### When to Delegate
When scraping a video detail page, check if the video iframe or embed matches any known host. If it does, **do not write custom scraping code**—delegate directly:
```kotlin
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val iframeUrl = doc.selectFirst("iframe")?.attr("src") ?: return false
    // Automatically unshortens shortlinks, matches regex, and calls the appropriate built-in extractor
    return loadExtractor(iframeUrl, data, subtitleCallback, callback)
}
```

---

## 🛠️ 3. Native JavaScript Engines (`JsInterpreter`, `JsHunter`, `JsUnpacker`)

CloudStream provides three native JavaScript decoders that run directly in Kotlin without requiring Android WebView:

### A. `JsInterpreter` (Native JS Execution)
Evaluates JavaScript expressions, loops, math, and string manipulation natively in Kotlin:
```kotlin
import com.lagradost.cloudstream3.utils.JsInterpreter

// Evaluate JS expression and retrieve output variable
val jsCode = """
    var a = 0x12;
    var b = 0x34;
    var streamKey = (a * b).toString(16);
"""
val result = JsInterpreter.evalJs(jsCode, variable = "streamKey")
// result => "3a8"
```

### B. `JsHunter` (H.U.N.T.E.R Obfuscation)
Reverses `eval(function(h,u,n,t,e,r))` packed scripts common on Asian and anime video hosts:
```kotlin
import com.lagradost.cloudstream3.utils.JsHunter

val hunter = JsHunter(scriptText)
if (hunter.detect()) {
    val unpackedCode = hunter.dehunt()
    // Extract m3u8 or video URLs from unpackedCode
}
```

### C. `JsUnpacker` (Dean Edwards P.A.C.K.E.R)
Reverses `eval(function(p,a,c,k,e,d))` packed scripts:
```kotlin
import com.lagradost.cloudstream3.utils.JsUnpacker

val unpacked = JsUnpacker(packedScript).unpack()
val m3u8Url = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""").find(unpacked.orEmpty())?.groupValues?.get(1)
```

---

## 🎬 4. Master HLS Resolution Parser (`M3u8Helper`)

Instead of manually parsing `#EXT-X-STREAM-INF` playlists, bandwidths, and qualities, use `M3u8Helper.generateM3u8`:
```kotlin
import com.lagradost.cloudstream3.utils.M3u8Helper

val links = M3u8Helper.generateM3u8(
    source = name,
    streamUrl = masterM3u8Url,
    referer = mainUrl,
    headers = mapOf("User-Agent" to "Mozilla/5.0 ...")
)
// Automatically creates ExtractorLink instances for 1080p, 720p, 480p, 360p
links.forEach { callback(it) }
```

---

## 🌐 5. Cloudflare & Headless Navigation (`WebViewResolver`)

For sites protected by Cloudflare Turnstile, browser checks, or JavaScript challenges:

```kotlin
import com.lagradost.cloudstream3.network.WebViewResolver

// 1. Declare in MainAPI
override val usesWebView = true

// 2. Use WebViewResolver to sniff network or solve challenge
val resolver = WebViewResolver(
    interceptUrl = Regex("""/player/index\.php\?data="""), // Stop when this request fires
    additionalUrls = listOf(Regex("""\.m3u8""")),          // Also capture stream URLs
    useOkhttp = false                                      // Disable OkHttp to bypass Cloudflare
)

// Intercept in OkHttp or execute standalone
val response = app.get(targetUrl, interceptor = resolver)
```

---

## 🎛️ 6. Full `MainAPI` Flags & Optimization Matrix

| Property | Type | Default | Purpose |
|---|---|---|---|
| `usesWebView` | `Boolean` | `false` | Enables Android WebView for Cloudflare Turnstile / JS bypass. |
| `sequentialMainPage` | `Boolean` | `false` | Serializes catalog rows to avoid HTTP 429 rate-limiting. |
| `sequentialMainPageDelay` | `Long` | `0L` | Delay in milliseconds between homepage row requests (e.g. `500L`). |
| `instantLinkLoading` | `Boolean` | `false` | Skips `loadLinks` and plays immediately from catalog URL (ideal for BDIX/FTP). |
| `hasQuickSearch` | `Boolean` | `false` | Enables instant search suggestions while user types in search bar. |
| `vpnStatus` | `VPNStatus` | `VPNStatus.None` | Signals app if VPN is recommended (`MightBeNeeded`, `Tor`). |
| `loadLinksTimeoutMs` | `Long?` | `null` | Per-provider timeout limit for link extraction (prevents UI hangs). |
| `getMainPageTimeoutMs` | `Long?` | `null` | Per-provider timeout limit for homepage loading. |
| `getVideoInterceptor(...)` | `Interceptor` | default | Injects custom headers (cookies, referer) into ExoPlayer for stream playback. |
