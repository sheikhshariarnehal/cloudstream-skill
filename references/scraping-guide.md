# Scraping Guide for CloudStream Providers

This is the practical technique behind every provider's `search`, `getMainPage`, `load`, and
`loadLinks`. Read the relevant section as needed; you don't need all of it at once.

Table of contents:
1. Two ways to pull data out of HTML: CSS selectors and regex
2. Parsing a site's own JSON API (Jackson in Kotlin)
3. Devtools-detection scripts and how to see past them
4. Disguising a scraper (headers, sessions, Cloudflare/captcha)
5. Finding the real video link

---

## 1. CSS selectors and regex

Scraping is just: fetch a page, then extract the piece of it you want. Open the browser's dev
tools (`F12` / `Ctrl+Shift+I`), use the element picker (`Ctrl+Shift+C`) to click the thing you
want, and look at the surrounding HTML.

**CSS selectors** parse the HTML like a browser would. A tag with `class="f4 mt-3"` is targeted
by `p.f4.mt-3` (one dot per class). You can sanity-check a selector in the browser console with
`document.querySelectorAll("p.f4.mt-3")`.

In Kotlin (Jsoup):
```kotlin
val doc = app.get(url).document
val text = doc.select("p.f4.mt-3").text().trim()
```
In Python (BeautifulSoup):
```python
soup = BeautifulSoup(response.text, "lxml")
text = soup.select("p.f4.mt-3")[0].text.strip()
```

**Regex** is useful when the data isn't in a clean, selectable tag, or is embedded inside a
`<script>` block as JS/JSON. Build and test the pattern on regex101.com against a full page dump
(`Ctrl+U` for view-source) before writing it into code. A pattern like:
```
<p class="f4 mt-3">\s*(.*)?\s*<
```
literally means: match the opening tag, skip whitespace, capture everything up to the next `<`.

**Important caveat**: content rendered by client-side JavaScript won't be present when you
`GET` the raw HTML from code — only what a browser would show after running scripts is visible in
dev tools. If a selector that works in the browser console returns nothing from your scraper,
suspect this first.

---

## 2. Using a site's JSON API directly

If the site's own frontend calls a JSON API (check the Network tab, filter by Fetch/XHR), prefer
that over scraping HTML — it's more stable and usually easier.

In Kotlin, model the JSON shape as a data class with Jackson annotations so the parser (and your
IDE) know the structure:

```kotlin
data class Planet(
    @JsonProperty("name") val name: String,
    @JsonProperty("population") val population: String,
    @JsonProperty("residents") val residents: List<String>,
)

val mapper: JsonMapper = JsonMapper.builder().addModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()

val planet = mapper.readValue<Planet>(jsonString)
```
(In an actual provider you'd usually use CloudStream's own helpers —
`app.get(url).parsed<Planet>()` or `AppUtils.parseJson<Planet>(jsonString)` — rather than building
a mapper by hand, but the underlying Jackson model is the same.)

Rules of thumb:
- You only need to declare the fields you actually use — partial data classes are fine.
- Any field that isn't present on every response **must** be typed nullable (`String?`), even
  with `FAIL_ON_UNKNOWN_PROPERTIES` set to false — that flag only tolerates *extra* fields, not
  *missing* ones. Tools like json2kt/quicktype can generate a first draft of the data class from
  a sample payload, but double-check their nullability guesses.

---

## 3. Devtools-detection scripts

Some sites try to stop you from inspecting their network traffic / JS at all. Common mechanisms:

- **Infinite `debugger` statements** — trivial to work around: right-click the offending line in
  Chrome and disable debugger statements from it, or disable the debugger entirely.
- **A custom `.toString()` on a value that's continuously logged** — once dev tools are open, any
  `console.log()` call resolves that custom `.toString()`, letting the page detect the instant
  dev tools opened.
- **A busy `while(true)` loop that only runs when a debugger is attached** — freezes the tab.

If you just need to see the network log without fighting this, a network-sniffer browser
extension that doesn't rely on the normal dev tools panel is the path of least resistance. Only
go further (patched browser builds disabling `devtools.console.bypass` /
`devtools.debugger.bypass` in Firefox's `about:config`) if you actually need step-through
debugging on a page that fights back this hard.

---

## 4. Disguising the scraper

Once you can see the request that matters, replaying it from code often gets rejected (HTTP
400–499, or sometimes a "successful" response that's actually an error page). Sites gate on
several signals:

| Header | What it's for | What to set it to |
|---|---|---|
| `User-Agent` | client identification | Copy your real browser's UA |
| `Referer` | where the request "came from" | The page you'd have actually clicked from |
| `X-Requested-With` | marks AJAX/API calls | Usually `XMLHttpRequest` |
| `Cookie` | session/access state | Whatever your browser had when it worked |
| `Authorization` | token/credential gating | The token/credential the site issued you |

Library choice matters too — some HTTP clients encode headers differently (fully vs. partially
encoded), which is enough by itself to make an otherwise-identical request get rejected by some
sites while another library's request succeeds. If a request fails inexplicably, trying a
different HTTP client is a cheap thing to test.

For sites that need cookie/challenge handling across many requests (Cloudflare-style checks,
hCaptcha/reCAPTCHA gates), the pattern is to wrap the session/client so every request checks the
response for a challenge and retries with fresh cookies/tokens once solved, rather than solving
the challenge ad hoc at each call site. Concretely: subclass the HTTP client's request method,
call `super`, inspect the response status/body, and if it indicates a challenge, resolve it,
persist the resulting cookies on the session, and retry — same idea as the callback-based
`ExtractorApi.getUrl` pattern CloudStream itself uses. Guard against infinite retry loops (cap
attempts, fall back to returning the original response) since a bypass that never succeeds would
otherwise recurse forever.

---

## 5. Finding the real video link

Video hosts are the most defended part of any streaming site — they pay for bandwidth and lose
that money whenever you bypass their ads, so expect obfuscation, encryption, referer/IP/time
locks, and occasionally a captcha directly on the link itself. You will essentially never find a
plain `<video src="...mp4">` in the raw HTML.

The general procedure:

1. **Find the iframe/video host.** Most sites load the player in an iframe pointed at a separate
   embed domain. In the Network tab, filter to Doc/HTML requests and look for anything not on the
   original domain.
2. **Open that iframe URL in its own tab.** This strips away everything irrelevant from the
   parent page and makes the embed's own requests much easier to read.
3. **Find the actual video request** — filter Media requests, or just look for anything ending in
   `.m3u8` or `.mp4`.
4. **Work backwards from that request.** Search for the video link's ID/token in earlier
   responses and headers to find where it originated, then repeat steps 1–4 on that origin if it
   turns out to be another redirect/embed layer.

**Spotting obfuscation while doing this:**
- **Base64** is everywhere. A telltale sign is a long string mixing upper/lowercase letters and
  digits, often ending in `==`. Decode anything that looks like this on sight.
- **AES encryption** shows up as a Base64 blob that decodes to garbage, usually alongside
  variable names like `enc`, `iv`, or a reference to `CryptoJS`. Find where decryption actually
  happens in the page's JS (a debugger breakpoint at the `CryptoJS` call site usually reveals the
  key) rather than trying to guess it.

**When a captcha guards the link itself**, in rough order of effort:
1. Try submitting an obviously-fake/empty captcha token — some backends don't actually validate
   it server-side.
2. Drive a real (or headless) browser/WebView for just that step, and hand its result back.
3. If it's a captcha with no real payload check, it may be possible to obtain a valid-looking key
   without a browser at all — see CloudStream's own example of this technique in `MainAPI.kt`
   (search the `cloudstream` repo history for the captcha-key handling around video link loading).

Reference walkthrough with screenshots (iframe hunting on Gogoanime-style sites, following a
redirector to the origin mp4 host):
https://recloudstream.github.io/csdocs/devs/scraping/finding_video_links/