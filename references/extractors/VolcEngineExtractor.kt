package com.lagradost.cloudstream3.extractors

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

/**
 * VolcEngine / ByteDance VOD Stream Extractor.
 * Used by modern short-drama platforms and Asian OTT apps.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class VolcEnginePlayInfoResponse(
    @JsonProperty("ResponseMetadata") val responseMetadata: VolcResponseMetadata? = null,
    @JsonProperty("Result") val result: VolcResult? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VolcResponseMetadata(
    @JsonProperty("RequestId") val requestId: String? = null,
    @JsonProperty("Action") val action: String? = null,
    @JsonProperty("Version") val version: String? = null,
    @JsonProperty("Service") val service: String? = null,
    @JsonProperty("Region") val region: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VolcResult(
    @JsonProperty("PlayInfoList") val playInfoList: List<VolcPlayInfo>? = null,
    @JsonProperty("VideoId") val videoId: String? = null,
    @JsonProperty("PosterUrl") val posterUrl: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VolcPlayInfo(
    @JsonProperty("MainPlayUrl") val mainPlayUrl: String? = null,
    @JsonProperty("BackupPlayUrl") val backupPlayUrl: String? = null,
    @JsonProperty("Bitrate") val bitrate: Int? = null,
    @JsonProperty("FileHash") val fileHash: String? = null,
    @JsonProperty("Format") val format: String? = null,
    @JsonProperty("Definition") val definition: String? = null,
    @JsonProperty("Width") val width: Int? = null,
    @JsonProperty("Height") val height: Int? = null,
    @JsonProperty("Url") val url: String? = null
)

class VolcEngineExtractor : ExtractorApi() {
    override val name = "VolcEngine"
    override val mainUrl = "https://vod.ap-southeast-1.volcengineapi.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url).text
        val playData = parseJson<VolcEnginePlayInfoResponse>(response)

        playData.result?.playInfoList?.forEach { playInfo ->
            val playUrl = playInfo.mainPlayUrl ?: playInfo.url ?: return@forEach
            val qualityInt = when (playInfo.definition?.lowercase()) {
                "1080p" -> Qualities.P1080.value
                "720p" -> Qualities.P720.value
                "480p" -> Qualities.P480.value
                "360p" -> Qualities.P360.value
                else -> Qualities.Unknown.value
            }

            val linkType = if (playUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "$name ${playInfo.definition ?: ""}",
                    url = playUrl,
                    type = linkType
                ) {
                    this.quality = qualityInt
                    this.referer = ""
                }
            )
        }
    }
}
