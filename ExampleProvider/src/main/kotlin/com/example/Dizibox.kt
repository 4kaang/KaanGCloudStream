package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Dizibox : MainAPI() {
    override var mainUrl = "https://www.dizibox.live"
    override var name = "Dizibox"
    override val supportedTypes = setOf(TvType.TvSeries)
    override var lang = "tr"
    override var hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val homeItems = ArrayList<HomePageList>()

        val series = document.select("article.post, div.tv-series-card, div.dizi-card").mapNotNull {
            it.toSearchResult()
        }

        if (series.isNotEmpty()) {
            homeItems.add(HomePageList("Son Eklenen Diziler", series))
        }

        return newHomePageResponse(homeItems)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, a")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let { 
            it.attr("data-src").ifEmpty { it.attr("src") } 
        }

        return newTvSeriesSearchResponse(title, fixUrl(href), TvType.TvSeries) {
            this.posterUrl = posterUrl?.let { fixUrl(it) }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document

        return document.select("article.post, div.tv-series-card, div.dizi-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: "Bilinmeyen Dizi"
        val poster = document.selectFirst("div.poster img, article img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val description = document.selectFirst("div.story, div.description, p")?.text()
        
        val episodes = document.select("div.episodes-list a, table.episodes a, div.season-episodes a, ul.bolumler a").mapNotNull { elem ->
            val epHref = elem.attr("href")
            val epName = elem.text()
            
            Episode(
                data = fixUrl(epHref),
                name = epName
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster?.let { fixUrl(it) }
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCdn: Boolean,
        handler: PlaylistUtils,
        videoCallback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframes = document.select("iframe")

        for (iframe in iframes) {
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                loadExtractor(fixUrl(src), data, subtitleCallback = {}, videoCallback = videoCallback)
            }
        }
        return true
    }
}
