package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

/**
 * GDMirror / Fast CDN Mirror Extractor.
 * Resolves direct video streams from file hosting mirrors.
 */
open class GDMirrorbot : ExtractorApi() {
    override var name = "GDMirror"
    override var mainUrl = "https://gdmirrorbot.nl"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        val downloadButton = doc.selectFirst("a:contains(Download), a.btn-download, a[href*='/download/']")
        val directUrl = downloadButton?.attr("href") ?: return

        callback.invoke(
            newExtractorLink(name, name, url = directUrl, type = ExtractorLinkType.VIDEO) {
                this.referer = url
                this.quality = Qualities.P1080.value
            }
        )
    }
}
