package com.lagradost.cloudstream3.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

data class AWSStreamResponse(
    @JsonProperty("videoSource") val videoSource: String? = null
)

class Zephyrflick : AWSStream() {
    override var name = "Zephyrflick"
    override var mainUrl = "https://as-cdn28.top"
    override val requiresReferer = true
}

class Ravok : AWSStream() {
    override var name = "Ravok"
    override var mainUrl = "https://ravok.buzz"
    override val requiresReferer = true
}

open class AWSStream : ExtractorApi() {
    override var name = "AWSStream"
    override var mainUrl = "https://z.awstream.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extractedHash = url.substringAfterLast("/")
        val doc = app.get(url).document
        val m3u8Url = "$mainUrl/player/index.php?data=$extractedHash&do=getVideo"
        val header = mapOf("x-requested-with" to "XMLHttpRequest")
        val formdata = mapOf("hash" to extractedHash, "r" to mainUrl)

        val response = app.post(m3u8Url, headers = header, data = formdata).parsedSafe<AWSStreamResponse>()
        response?.videoSource?.let { m3u8 ->
            callback.invoke(
                newExtractorLink(name, name, url = m3u8, type = ExtractorLinkType.M3U8) {
                    this.referer = ""
                    this.quality = Qualities.P1080.value
                }
            )

            val extractedPack = doc.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().orEmpty()
            JsUnpacker(extractedPack).unpack()?.let { unpacked ->
                val regex = Regex(""""kind"\s*:\s*"captions"\s*,\s*"file"\s*:\s*"(https[^"]+)"""")
                regex.findAll(unpacked).forEach { match ->
                    val subtitleUrl = match.groupValues[1]
                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = "English",
                            url = subtitleUrl
                        )
                    )
                }
            }
        }
    }
}
