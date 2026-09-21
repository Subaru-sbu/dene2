package com.example

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

@CloudstreamPlugin
class ArchivePlugin : Plugin() {
    override fun load(context: Context) = registerMainAPI(ArchiveProvider())
}

data class Res(val response: Docs? = null)
data class Docs(val docs: List<Doc>? = null)
data class Doc(val identifier: String? = null, val title: Any? = null)
data class Meta(val files: List<F>? = null)
data class F(val name: String? = null)

class ArchiveProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "Internet Archive"
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val q = URLEncoder.encode("title:($query) AND mediatype:movies", "UTF-8")
        val url = "$mainUrl/advancedsearch.php?q=$q&fl%5B%5D=identifier&fl%5B%5D=title&rows=20&output=json"
        return app.get(url).parsedSafe<Res>()?.response?.docs.orEmpty().mapNotNull {
            val id = it.identifier ?: return@mapNotNull null
            newMovieSearchResponse(it.title?.toString() ?: id, id, TvType.Movie) {
                posterUrl = "$mainUrl/services/img/$id"
            }
        }
    }

    override suspend fun load(url: String): LoadResponse =
        newMovieLoadResponse(url, url, TvType.Movie, url)

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ): Boolean {
        val file = app.get("$mainUrl/metadata/$data").parsedSafe<Meta>()
            ?.files?.firstOrNull { it.name?.endsWith(".mp4", true) == true } ?: return false
        val fileName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
        callback(newExtractorLink(name, name, "$mainUrl/download/$data/$fileName") {
            quality = Qualities.Unknown.value
        })
        return true
    }
}
