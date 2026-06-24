        package com.microtech.store (mts).anichin

        import com.lagradost.cloudstream3.*
        import com.lagradost.cloudstream3.utils.*
        import com.lagradost.cloudstream3.utils.AppUtils.parseJson
        import org.jsoup.Jsoup

        class AnichinProvider : MainAPI() {

            override var mainUrl        = "https://anichin.cafe"
            override var name           = "Anichin – Fansub Donghua Subtitle Indonesia"
            override val lang           = "id"
            override val hasMainPage    = true
            override val hasSearch      = true
            override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

            // ── Main Page ───────────────────────────────────────────────────

            override val mainPage = mainPageOf(
                "$url/movies/"   to "Filem Terbaru",
                "$url/tvshows/"  to "TV Series Terbaru",
                "$url/group_movie/malaysub/" to "MalaySub",
                "$url/group_movie/malaydub/" to "MalayDUB",
                "$url/genre/action/"         to "Aksi",
                "$url/genre/horror/"         to "Seram",
                "$url/genre/comedy/"         to "Komedi",
                "$url/genre/romance/"        to "Romantik",
                "$url/genre/animation/"      to "Animasi",
            )

            override suspend fun getMainPage(
                page: Int,
                request: MainPageRequest
            ): HomePageResponse {
                val pageUrl = request.data + if (page > 1) "page/$page/" else ""
                val items   = scrapeItems(pageUrl)
                return newHomePageResponse(request.name, items)
            }

            // ── Search ──────────────────────────────────────────────────────

            override suspend fun search(query: String): List<SearchResponse> {
                return scrapeItems("$url/?s=${query.replace(" ", "+")}")
            }

            // ── Scrape Helper ───────────────────────────────────────────────

            private suspend fun scrapeItems(url: String): List<SearchResponse> {
    val doc = app.get(url).document
    return doc.select(".item").mapNotNull {
        val a   = it.selectFirst("h2 a") ?: return@mapNotNull null
        val img = it.selectFirst("img")
        val src = img?.attr("data-src") ?: img?.attr("src") ?: ""
        newMovieSearchResponse(a.text().trim(), a.attr("href"), TvType.Movie) {
            this.posterUrl = src
        }
    }
}

            // ── Load Detail ─────────────────────────────────────────────────

            override suspend fun load(url: String): LoadResponse? {
    val doc    = app.get(url).document
    val title  = doc.selectFirst("h1.entry-title, h1.title, h1")?.text()?.trim() ?: return null
    val poster = doc.selectFirst(".post-thumbnail img, .entry-thumbnail img")?.attr("src")
    val plot   = doc.selectFirst(".entry-content p")?.text()?.trim()
    return newMovieLoadResponse(title, url, TvType.Movie, url) {
        this.posterUrl = poster
        this.plot      = plot
    }
}

            // ── Load Links ──────────────────────────────────────────────────

            override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val doc = app.get(data).document
    doc.select("iframe[src]").forEach {
        val src = it.attr("src")
        if (src.startsWith("http")) {
            loadExtractor(src, mainUrl, subtitleCallback, callback)
        }
    }
    return true
}
        }
