package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

/**
 * Upns / VidMoly / StreamRuby style packed player extractor.
 * Handles packed Javascript with regex-based M3U8 extraction.
 */
open class UpnsPlayer : ExtractorApi() {
    override var name = "Upns"
    override var mainUrl = "https://upns.org"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to (referer ?: mainUrl)
        )
        val response = app.get(url, headers = headers).text
        val packed = Regex("""eval\(function\(p,a,c,k,e,d\).*\)""").find(response)?.value

        val scriptContent = if (packed != null) {
            JsUnpacker(packed).unpack().orEmpty()
        } else {
            response
        }

        val m3u8Regex = Regex("""(?:file|source|src)\s*:\s*["']([^"']+\.m3u8[^"']*)["']""")
        m3u8Regex.findAll(scriptContent).forEach { match ->
            val streamUrl = match.groupValues[1]
            callback.invoke(
                newExtractorLink(name, name, url = streamUrl, type = ExtractorLinkType.M3U8) {
                    this.referer = url
                    this.quality = Qualities.P1080.value
                }
            )
        }
    }
}
