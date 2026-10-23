package com.lagradost.cloudstream3.extractors

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI

/**
 * HubCloud / GamerXYT / FSL Server Extractor.
 * Multi-mirror resolver for BuzzServer, PixelDrain, S3, and FSL CDNs.
 */
open class HubCloud : ExtractorApi() {
    override val name = "Hub-Cloud"
    override val mainUrl = "https://hubcloud.foo"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val tag = "HubCloud"
        val ref = "HubCloud"

        val uri = runCatching { URI(url) }.getOrElse {
            Log.e(tag, "Invalid URL: ${it.message}")
            return
        }

        val realUrl = uri.toString()
        val baseUrl = "${uri.scheme}://${uri.host}"

        val href = runCatching {
            if ("hubcloud.php" in realUrl) {
                realUrl
            } else {
                val doc = app.get(realUrl).document
                val raw = doc.selectFirst("#download")?.attr("href")
                    ?: doc.select("a[href]").firstOrNull { el ->
                        val h = el.attr("href")
                        h.contains("hubcloud.php") || h.contains("gamerxyt.com")
                    }?.attr("href")
                    ?: ""

                when {
                    raw.startsWith("http", true) -> raw
                    raw.isNotEmpty() -> baseUrl.trimEnd('/') + "/" + raw.trimStart('/')
                    else -> ""
                }
            }
        }.getOrElse {
            Log.e(tag, "Failed to extract href: ${it.message}")
            ""
        }

        if (href.isBlank()) return

        val document = app.get(href).document
        val size = document.selectFirst("i#size")?.text().orEmpty()
        val header = document.selectFirst("div.card-header")?.text().orEmpty()

        val quality = getQualityFromName(header)
        val labelExtras = buildString {
            if (header.isNotEmpty()) append(" [$header]")
            if (size.isNotEmpty()) append(" [$size]")
        }

        document.select("a.btn[href]").forEach { element ->
            val link = element.attr("href")
            val label = element.ownText().lowercase()

            when {
                "fsl server" in label || "fslv2" in label -> {
                    callback(
                        newExtractorLink(
                            "$ref [FSL Server]",
                            "$ref [FSL Server]$labelExtras",
                            link
                        ) { this.quality = quality }
                    )
                }

                "download file" in label -> {
                    callback(
                        newExtractorLink(
                            ref,
                            "$ref$labelExtras",
                            link
                        ) { this.quality = quality }
                    )
                }

                "buzzserver" in label -> {
                    val resp = app.get("$link/download", referer = link, allowRedirects = false)
                    val dlink = resp.headers["hx-redirect"]
                        ?: resp.headers["HX-Redirect"].orEmpty()

                    if (dlink.isNotBlank()) {
                        callback(
                            newExtractorLink(
                                "$ref [BuzzServer]",
                                "$ref [BuzzServer]$labelExtras",
                                dlink
                            ) { this.quality = quality }
                        )
                    }
                }

                "pixeldra" in label || "pixelserver" in label -> {
                    val finalUrl = if ("download" in link) link else {
                        "https://pixeldrain.com/api/file/${link.substringAfterLast("/")}?download"
                    }
                    callback(
                        newExtractorLink(
                            "$ref Pixeldrain",
                            "$ref Pixeldrain$labelExtras",
                            finalUrl
                        ) { this.quality = quality }
                    )
                }

                "s3 server" in label -> {
                    callback(
                        newExtractorLink(
                            "$ref [S3 Server]",
                            "$ref [S3 Server]$labelExtras",
                            link
                        ) { this.quality = quality }
                    )
                }
            }
        }
    }
}
