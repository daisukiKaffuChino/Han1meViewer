package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.GETCHU_BASE_URL
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreview
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.regex.Pattern
import kotlin.runCatching

object GetchuParser {
    fun getchuPreview(body: String, dateCode: String): WebsiteState<GetchuPreview> {
        val parseBody = Jsoup.parse(body, GETCHU_BASE_URL).body()
        LogUtil.d(
            "GetchuPreviewParser",
            "parse list date=$dateCode bodyLength=${body.length} title=${
                parseBody.ownerDocument()?.title()
            } " +
                    "dateHeaders=${parseBody.select("div.category_anime_t2").size} " +
                    "products=${parseBody.select("div.div_product").size} " +
                    "softLinks=${parseBody.select("a[href*=/soft.phtml], a[href*=/item/]").size}"
        )
        val groups = mutableListOf<GetchuPreview.Group>()
        parseBody.select("div.category_anime_t2").forEach { header ->
            val releaseDate = header.text().replace("発売タイトル", EMPTY_STRING).trim()
            val container = header.parent()?.nextCategoryAnimeBody()
            LogUtil.d(
                "GetchuPreviewParser",
                "group header=${header.text().cleanGetchuText()} releaseDate=$releaseDate " +
                        "containerFound=${container != null} containerProducts=${container?.select("div.div_product")?.size ?: 0}"
            )
            val items = container?.select("div.div_product")?.mapNotNull { product ->
                val link =
                    product.selectFirst("td.dd > a[href*=/soft.phtml], td.dd > a[href*=/item/]")
                        ?: product.selectFirst("a[href*=/soft.phtml], a[href*=/item/]")
                        ?: return@mapNotNull null
                val detailUrl = link.absUrl("href").normalizeGetchuDetailUrl()
                val id = detailUrl.getGetchuId() ?: return@mapNotNull null
                val titleAndBrand = link.text().cleanGetchuText()
                val brand = titleAndBrand.substringAfterLast("(", EMPTY_STRING)
                    .substringBeforeLast(")", EMPTY_STRING)
                    .takeIf { it.isNotBlank() }
                val title = titleAndBrand.replace(Regex("\\s*\\([^()]+\\)\\s*$"), EMPTY_STRING)
                    .trim()
                    .ifBlank {
                        product.selectFirst("img")?.attr("alt")?.cleanGetchuText().orEmpty()
                    }
                val coverUrl = product.selectFirst("img[src*=package]")?.absUrl("src")
                    ?.replace("package_s.", "package.")
                    ?.withGetchuGc()
                val price = product.selectFirst("span.redb")?.text()?.cleanGetchuText()
                GetchuPreview.Item(
                    id = id,
                    title = title,
                    brand = brand,
                    coverUrl = coverUrl,
                    detailUrl = detailUrl,
                    price = price,
                )
            }.orEmpty().distinctBy { it.id }
            if (items.isNotEmpty()) {
                groups.add(GetchuPreview.Group(releaseDate = releaseDate, items = items))
            }
        }
        LogUtil.d(
            "GetchuPreviewParser",
            "parse list result date=$dateCode groups=${groups.size} totalItems=${groups.sumOf { it.items.size }}"
        )

        return WebsiteState.Success(GetchuPreview(dateCode = dateCode, groups = groups))
    }

    fun getchuPreviewDetail(body: String, id: String): WebsiteState<GetchuPreviewDetail> {
        val parseBody = Jsoup.parse(body, GETCHU_BASE_URL).body()
        LogUtil.d(
            "GetchuPreviewParser",
            "parse detail id=$id bodyLength=${body.length} title=${
                parseBody.ownerDocument()?.title()
            } " +
                    "jsonLd=${parseBody.select("script[type*=ld+json]").size} " +
                    "tableTitles=${parseBody.select("h3.tabletitle").size} " +
                    "tableBodies=${parseBody.select(".tablebody").size} " +
                    "rawTableBodies=${
                        Regex(
                            "class=[\"'][^\"']*tablebody",
                            RegexOption.IGNORE_CASE
                        ).findAll(body).count()
                    } " +
                    "rawReleaseDate=${body.contains("_crelease_date")} " +
                    "samples=${parseBody.select("div.item-Samplecard a[href]").size} " +
                    "videos=${parseBody.select("video source[src], video[src]").size}"
        )
        val productJson = parseBody.select("script[type*=ld+json]")
            .asSequence()
            .mapNotNull { script ->
                val jsonText = script.data().ifBlank { script.html() }.trim()
                runCatching { JSONObject(jsonText) }.getOrNull()
            }
            .plus(body.extractGetchuJsonLdObjects())
            .flatMap { json ->
                val graph = json.optJSONArray("@graph")
                (0 until (graph?.length() ?: 0)).asSequence()
                    .mapNotNull { graph?.optJSONObject(it) }
            }
            .firstOrNull { it.optString("@type") == "Product" }

        val title = productJson?.optString("name")?.cleanGetchuText()?.takeIf { it.isNotBlank() }
            ?: parseBody.getElementById("soft-title")?.text()?.cleanGetchuText()
                ?.takeIf { it.isNotBlank() }
            ?: parseBody.selectFirst("meta[property=og:title]")?.attr("content")?.cleanGetchuText()
            ?: parseBody.ownerDocument()?.title()?.substringBefore(" | ")?.cleanGetchuText()
            ?: EMPTY_STRING
        val specMap = parseBody.extractGetchuSpecMap() + body.extractGetchuSpecMapFromRawHtml()
        val brand = productJson?.optJSONObject("brand")?.optString("name")?.cleanGetchuText()
            ?.takeIf { it.isNotBlank() }
            ?: parseBody.getElementById("brandsite")?.text()?.cleanGetchuText()
                ?.takeIf { it.isNotBlank() }
            ?: specMap.firstValueContains("ブランド")
        val coverUrl = productJson?.optJSONArray("image")?.optString(0)?.toGetchuAbsUrl()
            ?: parseBody.selectFirst("meta[property=og:image]")?.attr("content")?.toGetchuAbsUrl()
            ?: body.extractMetaContent("og:image")?.toGetchuAbsUrl()
            ?: body.extractMetaContent("twitter:image:src")?.toGetchuAbsUrl()
            ?: parseBody.selectFirst("#soft_table img[src*=package]")?.absUrl("src")
                ?.toGetchuAbsUrl()
            ?: Regex("href=[\"']([^\"']*?/brandnew/$id/[^\"']*?package\\.jpg)[\"']")
                .find(body)?.groupValues?.getOrNull(1)?.toGetchuAbsUrl()
        val metaDescription =
            productJson?.optString("description")?.cleanGetchuText()?.takeIf { it.isNotBlank() }
                ?: parseBody.selectFirst("meta[name=description]")?.attr("content")
                    ?.cleanGetchuText()
        val offer = productJson?.optJSONObject("offers")
        val price = offer?.optString("price")?.takeIf { it.isNotBlank() }?.let { "¥$it" }
            ?: parseBody.selectFirst("span.redb2")?.text()?.cleanGetchuText()
            ?: specMap.firstValueContains("価格")
            ?: parseBody.select("#soft_table tr").firstNotNullOfOrNull { row ->
                row.text().cleanGetchuText()
                    .takeIf { text -> text.contains("¥") || text.contains("￥") || text.contains("円") }
            }
        val productUrl = offer?.optString("url")?.takeIf { it.isNotBlank() }
            ?: "${GETCHU_BASE_URL}item/$id/"

        val releaseDateRegex =
            Regex("[\"']?_crelease_date[\"']?\\s*:\\s*[\"']?([0-9]{4}-[0-9]{2}-[0-9]{2})")
        val releaseDate = parseBody.select("script").firstNotNullOfOrNull { script ->
            releaseDateRegex.find(script.data())?.groupValues?.getOrNull(1)
        } ?: parseBody.selectFirst("a[href*=start_date]")?.text()?.cleanGetchuText()
        ?: specMap.firstValueContains("発売日")
        ?: releaseDateRegex.find(body)?.groupValues?.getOrNull(1)
        ?: Regex("start_date=([0-9]{4}(?:/|%2F)[0-9]{2}(?:/|%2F)[0-9]{2})", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)?.replace("%2F", "/", ignoreCase = true)
        ?: body.extractNearbyDate("発売日")

        val videoUrls = (parseBody.select("video source[src], video[src]")
            .mapNotNull { it.attr("src").toGetchuAbsUrl() }
                + body.extractGetchuVideoUrls())
            .distinct()

        val sections = (
                parseBody.extractGetchuTextSections() +
                        body.extractGetchuTextSectionsFromRawHtml() +
                        body.extractGetchuTextSectionsByBlocks() +
                        body.extractGetchuTextSectionsByOrder() +
                        body.extractGetchuLooseTextSectionsByOrder()
                ).distinctBy { it.title to it.body }
            .sortedWith(
                compareBy { section ->
                    when {
                        section.title.contains("商品紹介") -> 0
                        section.title.contains("ストーリー") -> 1
                        else -> 2
                    }
                }
            )
        val description = sections
            .filter { it.title.contains("商品紹介") || it.title.contains("ストーリー") }
            .filterNot { it.title.contains("商品一覧") || it.title.contains("関連商品") }
            .joinToString("\n\n") { it.body }
            .takeIf { it.isNotBlank() }
            ?: metaDescription

        val jsonLdImages = productJson?.optJSONArray("image")?.let { images ->
            (0 until images.length()).mapNotNull { index ->
                images.optString(index).toGetchuAbsUrl()?.withGetchuGc()
            }
        }.orEmpty().drop(1)
        val sampleImages = (parseBody.select("div.item-Samplecard a[href]")
            .mapNotNull { it.absUrl("href").takeIf(String::isNotBlank) }
                + body.extractGetchuSampleImages(id)
                + jsonLdImages)
            .map { it.withGetchuGc() }
            .distinct()

        val seriesItems = parseBody.select("div.item-series-content")
            .mapNotNull { it.toGetchuRelatedItem() }
            .distinctBy { it.id }
            .filterNot { it.id == id }

        LogUtil.d(
            "GetchuPreviewParser",
            "parse detail result id=$id title=${title.take(80)} sections=${sections.size} " +
                    "sectionTitles=${sections.joinToString { it.title }} brand=$brand releaseDate=$releaseDate price=$price samples=${sampleImages.size} " +
                    "videos=${videoUrls.size} series=${seriesItems.size} related=${seriesItems.size}"
        )

        return WebsiteState.Success(
            GetchuPreviewDetail(
                id = id,
                title = title,
                brand = brand,
                coverUrl = coverUrl?.withGetchuGc(),
                description = description,
                releaseDate = releaseDate,
                price = price,
                productUrl = productUrl,
                videoUrls = videoUrls,
                sections = sections,
                sampleImages = sampleImages,
                seriesItems = seriesItems,
                relatedItems = seriesItems,
            )
        )
    }

    private fun Element.toGetchuRelatedItem(): GetchuPreview.Item? {
        val link = selectFirst("a[href]") ?: return null
        val detailUrl = link.absUrl("href").normalizeGetchuDetailUrl()
        val id = detailUrl.getGetchuId() ?: return null
        val image = selectFirst("img")
        val title = image?.attr("alt")?.replace("パッケージ画像", EMPTY_STRING)?.cleanGetchuText()
            ?.takeIf { it.isNotBlank() }
            ?: link.text().cleanGetchuText()
        val brand = selectFirst(".table-003-brand")?.text()
            ?.trim('(', ')')
            ?.cleanGetchuText()
            ?.takeIf { it.isNotBlank() }
        val releaseDate = selectFirst(".table-003-releasedate")?.text()?.cleanGetchuText()
        val price =
            selectFirst("span.redb, span.gr_soft_carousel_price_num")?.text()?.cleanGetchuText()
        return GetchuPreview.Item(
            id = id,
            title = title,
            brand = brand ?: releaseDate,
            coverUrl = image?.absUrl("src")?.withGetchuGc(),
            detailUrl = detailUrl,
            price = price,
        )
    }

    fun getchuSeriesItems(body: String): List<GetchuPreview.Item> {
        val parseBody = Jsoup.parse(body, GETCHU_BASE_URL).body()
        return parseBody.select("div.item-series-content, div[class*=item-series]")
            .mapNotNull { it.toGetchuRelatedItem() }
            .distinctBy { it.id }
    }

    private fun Element.nextCategoryAnimeBody(): Element? {
        var next = nextElementSibling()
        while (next != null && !next.hasClass("category_anime_b")) {
            next = next.nextElementSibling()
        }
        return next
    }

    private fun Element.extractGetchuSpecMap(): Map<String, String> {
        return select("#soft_table tr").mapNotNull { row ->
            val cells = row.children().filter { it.`is`("td, th") }
            if (cells.size < 2) return@mapNotNull null
            val key = cells[0].text().cleanGetchuText()
                .replace(Regex("[：:]"), EMPTY_STRING)
                .replace(Regex("[\\u3000\\s]+"), EMPTY_STRING)
            val value = cells[1].text().cleanGetchuText()
            if (key.isBlank() || value.isBlank()) return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun String.extractGetchuSpecMapFromRawHtml(): Map<String, String> {
        return mapOf(
            "ブランド" to Regex(
                "<td[^>]*>\\s*ブランド[:：]\\s*</td>\\s*<td[^>]*>([\\s\\S]*?)</td>",
                RegexOption.IGNORE_CASE
            ),
            "定価" to Regex(
                "<td[^>]*>\\s*定価[:：]\\s*</td>\\s*<td[^>]*>([\\s\\S]*?)</td>",
                RegexOption.IGNORE_CASE
            ),
            "発売日" to Regex(
                "<td[^>]*>\\s*発売日[:：]\\s*</td>\\s*<td[^>]*>([\\s\\S]*?)</td>",
                RegexOption.IGNORE_CASE
            ),
        ).mapNotNull { (key, regex) ->
            val rawValue = regex.find(this)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            val value = Jsoup.parse(rawValue).text().cleanGetchuText()
            if (value.isBlank()) null else key to value
        }.toMap()
    }

    private fun String.extractNearbyDate(label: String): String? {
        val index = indexOf(label)
        if (index < 0) return null
        return Regex("[0-9]{4}/[0-9]{2}/[0-9]{2}")
            .find(substring(index, (index + 800).coerceAtMost(length)))
            ?.value
    }

    private fun Map<String, String>.firstValueContains(keyword: String): String? {
        return entries.firstOrNull { (key, _) -> key.contains(keyword) }?.value
    }

    private fun Element.extractGetchuTextSections(): List<GetchuPreviewDetail.TextSection> {
        val contentTitles = select("h3.tabletitle")
            .filterNot { title ->
                title.parents().any { it.`is`("section.related-products, section.brand-products") }
            }
        val sections = contentTitles.mapNotNull { titleElement ->
            val bodyElement =
                titleElement.nextElementSiblings().firstOrNull { it.hasClass("tablebody") }
                    ?: titleElement.parent()?.children()?.dropWhile { it != titleElement }?.drop(1)
                        ?.firstOrNull { it.hasClass("tablebody") }
                    ?: return@mapNotNull null
            val sectionTitle = titleElement.text().replace("&nbsp;", EMPTY_STRING).cleanGetchuText()
            val sectionBody = bodyElement.text().cleanGetchuText()
            if (sectionTitle.isBlank() || sectionBody.isBlank()) return@mapNotNull null
            GetchuPreviewDetail.TextSection(sectionTitle, sectionBody)
        }.filter { section ->
            section.title.contains("商品紹介") || section.title.contains("ストーリー") || section.title.contains(
                "スタッフ"
            )
        }.distinctBy { it.title to it.body }

        return sections.sortedWith(
            compareBy { section ->
                when {
                    section.title.contains("商品紹介") -> 0
                    section.title.contains("ストーリー") -> 1
                    else -> 2
                }
            }
        )
    }

    private fun String.extractGetchuJsonLdObjects(): Sequence<JSONObject> {
        return Regex(
            "<script[^>]*type=[\"'][^\"']*ld\\+json[^\"']*[\"'][^>]*>([\\s\\S]*?)</script>",
            RegexOption.IGNORE_CASE,
        ).findAll(this).mapNotNull { match ->
            runCatching { JSONObject(match.groupValues[1].trim()) }.getOrNull()
        }
    }

    private fun String.extractMetaContent(propertyOrName: String): String? {
        val escaped = Pattern.quote(propertyOrName)
        val propertyFirst = Regex(
            "<meta[^>]*(?:property|name)=[\"']$escaped[\"'][^>]*content=[\"']([^\"']+)[\"'][^>]*>",
            RegexOption.IGNORE_CASE,
        )
        val contentFirst = Regex(
            "<meta[^>]*content=[\"']([^\"']+)[\"'][^>]*(?:property|name)=[\"']$escaped[\"'][^>]*>",
            RegexOption.IGNORE_CASE,
        )
        return propertyFirst.find(this)?.groupValues?.getOrNull(1)
            ?: contentFirst.find(this)?.groupValues?.getOrNull(1)
    }

    private fun String.extractGetchuTextSectionsFromRawHtml(): List<GetchuPreviewDetail.TextSection> {
        return Regex(
            "<h3[^>]*class=[\"'][^\"']*tabletitle[^\"']*[\"'][^>]*>([\\s\\S]*?)</h3>\\s*<div[^>]*class=[\"'][^\"']*tablebody[^\"']*[\"'][^>]*>([\\s\\S]*?)</div>",
            RegexOption.IGNORE_CASE,
        ).findAll(this).mapNotNull { match ->
            val title = Jsoup.parse(match.groupValues[1]).text()
                .replace("&nbsp;", EMPTY_STRING)
                .cleanGetchuText()
            val body = Jsoup.parse(match.groupValues[2].replace("<br>", "\n", ignoreCase = true))
                .text()
                .cleanGetchuText()
            if (title.isBlank() || body.isBlank()) return@mapNotNull null
            GetchuPreviewDetail.TextSection(title, body)
        }.filter { section ->
            section.title.contains("商品紹介") || section.title.contains("ストーリー") || section.title.contains(
                "スタッフ"
            )
        }.sortedWith(
            compareBy { section ->
                when {
                    section.title.contains("商品紹介") -> 0
                    section.title.contains("ストーリー") -> 1
                    else -> 2
                }
            }
        ).distinctBy { it.title to it.body }.toList()
    }

    private fun String.extractGetchuTextSectionsByBlocks(): List<GetchuPreviewDetail.TextSection> {
        val titleMatches = Regex(
            "<h3[^>]*class=[\"'][^\"']*tabletitle[^\"']*[\"'][^>]*>([\\s\\S]*?)</h3>",
            RegexOption.IGNORE_CASE,
        ).findAll(this).toList()
        return titleMatches.mapIndexedNotNull { index, match ->
            val title = Jsoup.parse(match.groupValues[1]).text()
                .replace("&nbsp;", EMPTY_STRING)
                .cleanGetchuText()
            if (!title.contains("商品紹介") && !title.contains("ストーリー") && !title.contains("スタッフ")) {
                return@mapIndexedNotNull null
            }
            val nextStart = titleMatches.getOrNull(index + 1)?.range?.first ?: length
            val block = substring(match.range.last + 1, nextStart)
            val tableBody = Regex(
                "<div[^>]*class=[\"'][^\"']*tablebody[^\"']*[\"'][^>]*>([\\s\\S]*?)(?:</div>|$)",
                RegexOption.IGNORE_CASE,
            ).find(block)?.groupValues?.getOrNull(1) ?: block
            val body =
                Jsoup.parse(tableBody.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n"))
                    .text()
                    .cleanGetchuText()
            if (body.isBlank()) return@mapIndexedNotNull null
            GetchuPreviewDetail.TextSection(title, body)
        }.sortedWith(
            compareBy { section ->
                when {
                    section.title.contains("商品紹介") -> 0
                    section.title.contains("ストーリー") -> 1
                    else -> 2
                }
            }
        ).distinctBy { it.title to it.body }
    }

    private fun String.extractGetchuTextSectionsByOrder(): List<GetchuPreviewDetail.TextSection> {
        val titleMatches = Regex(
            "<h3[^>]*class=[\"'][^\"']*tabletitle[^\"']*[\"'][^>]*>([\\s\\S]*?)</h3>",
            RegexOption.IGNORE_CASE,
        ).findAll(this).toList()
        val textBlocks = titleMatches.mapIndexedNotNull { index, match ->
            val nextStart = titleMatches.getOrNull(index + 1)?.range?.first ?: length
            val block = substring(match.range.last + 1, nextStart)
            if (block.contains("<video", ignoreCase = true) || block.contains(
                    "item-Samplecard",
                    ignoreCase = true
                )
            ) {
                return@mapIndexedNotNull null
            }
            val bodyHtml = Regex(
                "<div[^>]*class=[\"'][^\"']*tablebody[^\"']*[\"'][^>]*>([\\s\\S]*?)(?:</div>|$)",
                RegexOption.IGNORE_CASE,
            ).find(block)?.groupValues?.getOrNull(1) ?: return@mapIndexedNotNull null
            val body =
                Jsoup.parse(bodyHtml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n"))
                    .text()
                    .cleanGetchuText()
            if (body.length < 20) return@mapIndexedNotNull null
            body
        }.take(2)

        return textBlocks.mapIndexed { index, body ->
            GetchuPreviewDetail.TextSection(
                title = if (index == 0) "商品紹介" else "ストーリー",
                body = body,
            )
        }
    }

    private fun String.extractGetchuLooseTextSectionsByOrder(): List<GetchuPreviewDetail.TextSection> {
        val titleMatches = Regex(
            "<h3[^>]*class=[\"'][^\"']*tabletitle[^\"']*[\"'][^>]*>([\\s\\S]*?)</h3>",
            RegexOption.IGNORE_CASE,
        ).findAll(this).toList()

        val ignoredBlockMarkers = listOf(
            "<video",
            "item-Samplecard",
            "related-products",
            "brand-products",
            "getchu-search-container",
            "item-series",
            "GetchuSearchAjax",
        )
        val ignoredTitleKeywords = listOf(
            "動画",
            "サンプル",
            "特典",
            "商品一覧",
            "関連商品",
        )

        return titleMatches.mapIndexedNotNull { index, match ->
            val title = Jsoup.parse(match.groupValues[1])
                .text()
                .replace("&nbsp;", EMPTY_STRING)
                .cleanGetchuText()
            if (title.isBlank() || ignoredTitleKeywords.any { title.contains(it) }) return@mapIndexedNotNull null

            val nextStart = titleMatches.getOrNull(index + 1)?.range?.first ?: length
            val block = substring(match.range.last + 1, nextStart)
            if (ignoredBlockMarkers.any {
                    block.contains(
                        it,
                        ignoreCase = true
                    )
                }) return@mapIndexedNotNull null

            val body =
                Jsoup.parse(block.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n"))
                    .text()
                    .cleanGetchuText()
            if (body.length < 20) return@mapIndexedNotNull null
            GetchuPreviewDetail.TextSection(title, body)
        }.take(3).distinctBy { it.title to it.body }
    }

    private fun String.extractGetchuSampleImages(id: String): List<String> {
        return Regex("[\"']([^\"']*?/brandnew/$id/c${id}sample\\d+\\.jpg)[\"']")
            .findAll(this)
            .mapNotNull { it.groupValues.getOrNull(1)?.toGetchuAbsUrl() }
            .toList()
    }

    private fun String.extractGetchuVideoUrls(): List<String> {
        return Regex("(?:https?:)?//[^\"'<>\\s]+?\\.mp4(?:\\?[^\"'<>\\s]*)?", RegexOption.IGNORE_CASE)
            .findAll(replace("\\/", "/"))
            .map { it.value }
            .map { if (it.startsWith("//")) "https:$it" else it }
            .toList()
    }

    private fun String.cleanGetchuText(): String {
        return replace('\u00a0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String?.toGetchuAbsUrl(): String? {
        if (this.isNullOrBlank()) return null
        return when {
            startsWith("//") -> "https:$this"
            startsWith("/") -> GETCHU_BASE_URL.trimEnd('/') + this
            startsWith("http") -> this
            else -> GETCHU_BASE_URL + this.trimStart('/')
        }
    }

    private fun String.withGetchuGc(): String {
        if (!startsWith(GETCHU_BASE_URL) || contains("gc=gc")) return this
        val separator = if ('?' in this) "&" else "?"
        return "$this${separator}gc=gc"
    }

    private fun String.normalizeGetchuDetailUrl(): String {
        val id = getGetchuId() ?: return this
        return "${GETCHU_BASE_URL}item/$id/"
    }

    private fun String.getGetchuId(): String? {
        return Regex("(?:id=|/item/)(\\d+)").find(this)?.groupValues?.getOrNull(1)
    }
}
