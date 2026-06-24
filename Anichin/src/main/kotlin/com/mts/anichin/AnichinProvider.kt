package com.mts.anichin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class AnichinProvider : MainAPI() {

    override var mainUrl        = "https://anichin.cafe"
    override var name           = "Anichin – Fansub Donghua Subtitle Indonesia"
    override val lang           = "id"
    override val hasMainPage    = true
    override val hasSearch      = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "https://anichin.cafe/movies/"               to "Filem Terbaru",
        "https://anichin.cafe/tvshows/"              to "TV Series Terbaru",
        "https://anichin.cafe/group_movie/malaysub/" to "MalaySub",
        "https://anichin.cafe/group_movie/malaydub/" to "MalayDUB",
        "https://anichin.cafe/genre/action/"         to "Aksi",
        "https://anichin.cafe/genre/horror/"         to "Seram",
        "https://anichin.cafe/genre/comedy/"         to "Komedi",
        "https://anichin.cafe/genre/animation/"      to "Animasi",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = request.data + if (page > 1) "page/$page/" else ""
        return newHomePageResponse(request.name, scrapeList(pageUrl))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return scrapeList("$mainUrl/?s=${query.replace(" ", "+")}")
    }
    
    private suspend fun scrapeList(pageUrl: String): List<SearchResponse> {
        val doc = app.get(pageUrl).document
        return doc.select("article, .post, .item").mapNotNull {
            val a   = it.selectFirst("h2 a, h3 a, a") ?: return@mapNotNull null
            val img = it.selectFirst("img")
            val src = img?.attr("data-src") ?: img?.attr("src") ?: ""
            newMovieSearchResponse(a.text().trim(), a.attr("href"), TvType.Movie) { posterUrl = src }
        }
    }
    
    override suspend fun load(url: String): LoadResponse? {
        val doc    = app.get(url).document
        val title  = doc.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = doc.selectFirst(".post-thumbnail img")?.attr("src")
        val plot   = doc.selectFirst(".entry-content p")?.text()?.trim()
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster; this.plot = plot
        }
    }
    
    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("iframe[src]").forEach {
            val src = it.attr("src")
            if (src.startsWith("http")) loadExtractor(src, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
