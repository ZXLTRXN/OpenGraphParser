package com.kedia.ogparser

import android.util.Log
import org.jsoup.Jsoup

/**
 * @param timeout - Timeout for requests, specified in milliseconds (default - 60000)
 * @param jsoupProxy - Specify proxy for requests (host, port)
 * @param maxBodySize - The maximum size to fetch for body
 */

class JsoupNetworkCall(
    private val timeout: Int? = DEFAULT_TIMEOUT,
    private val jsoupProxy: JsoupProxy? = null,
    private val maxBodySize: Int? = null
) {

    fun callUrl(url: String, agent: String): OpenGraphResult? {
        val openGraphResult = OpenGraphResult()
        try {
            val connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(agent)
                .referrer(REFERRER)
                .timeout(timeout ?: DEFAULT_TIMEOUT)
                .followRedirects(true)

            jsoupProxy?.let { connection.proxy(it.host, it.port) }
            maxBodySize?.let { connection.maxBodySize(it) }

            val response = connection.execute()

            val doc = response.parse()
            val ogTags = doc.select(DOC_SELECT_OGTAGS)

            ogTags.forEach { tag ->
                val text = tag.attr(PROPERTY)

                when (text) {
                    OG_IMAGE -> {
                        openGraphResult.image = tag.attr(OPEN_GRAPH_KEY)
                    }
                    OG_DESCRIPTION -> {
                        openGraphResult.description = tag.attr(OPEN_GRAPH_KEY)
                    }
                    OG_URL -> {
                        openGraphResult.url = tag.attr(OPEN_GRAPH_KEY)
                    }
                    OG_TITLE -> {
                        openGraphResult.title = tag.attr(OPEN_GRAPH_KEY)
                    }
                    OG_SITE_NAME -> {
                        openGraphResult.siteName = tag.attr(OPEN_GRAPH_KEY)
                    }
                    OG_TYPE -> {
                        openGraphResult.type = tag.attr(OPEN_GRAPH_KEY)
                    }
                }
            }

            if (openGraphResult.title.isNullOrEmpty()) {
                openGraphResult.title = doc.title()
            }

            if (openGraphResult.description.isNullOrEmpty()) {
                openGraphResult.description = doc.extractDescriptionNotFromOg()
            }

            if (openGraphResult.image.isNullOrEmpty()) {
                openGraphResult.image = doc.extractImageNotFromPropertyOg()
            }
            
            if (openGraphResult.url.isNullOrEmpty()) {
                openGraphResult.url = getBaseUrl(url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return openGraphResult
    }

    
    private fun Document.extractDescriptionNotFromOg(): String {
        var result: String? = select(DOC_SELECT_DESCRIPTION_1)
            .firstOrNull()
            ?.attr(CONTENT_KEY) ?: ""


        if (result.isNullOrEmpty()) {
            result = select(DOC_SELECT_DESCRIPTION_2)
                .firstOrNull()
                ?.attr(CONTENT_KEY) ?: ""
        }
        return result
    }

    private fun Document.extractImageNotFromPropertyOg(): String? {
        var result: String? = select("meta[name=$OG_IMAGE]")
            .firstOrNull()
            ?.attr(OPEN_GRAPH_KEY)

        if (result.isNullOrEmpty()) {
            result = select(DOC_SELECT_IMAGE_SRC)
                .firstOrNull()
                ?.attr(HREF_KEY)
        }

        if (result.isNullOrEmpty()) {
            result = select(DOC_SELECT_TOUCH_ICON)
                .firstOrNull()
                ?.attr(HREF_KEY)
        }

        if (result.isNullOrEmpty()) {
            result = select(DOC_SELECT_ICON)
                .firstOrNull()
                ?.attr(HREF_KEY)
        }

        return if (result.isNullOrEmpty()) null
        else resolveUrl(this@extractImageNotFromPropertyOg.baseUri(), result)
    }

    companion object {
        private const val REFERRER = "http://www.google.com"
        private const val DEFAULT_TIMEOUT = 60000

        private const val DOC_SELECT_OGTAGS = "meta[property^=og:]"
        private const val DOC_SELECT_DESCRIPTION_1 = "meta[name=description]"
        private const val DOC_SELECT_DESCRIPTION_2 = "meta[name=Description]"

        private const val DOC_SELECT_IMAGE_SRC = "link[rel=image_src]"
        private const val DOC_SELECT_TOUCH_ICON = "link[rel=apple-touch-icon]"
        private const val DOC_SELECT_ICON = "link[rel=icon]"

        private const val CONTENT_KEY = "content"
        private const val HREF_KEY = "href"
        private const val OPEN_GRAPH_KEY = CONTENT_KEY
        private const val PROPERTY = "property"

        private const val OG_IMAGE = "og:image"
        private const val OG_DESCRIPTION = "og:description"
        private const val OG_URL = "og:url"
        private const val OG_TITLE = "og:title"
        private const val OG_SITE_NAME = "og:site_name"
        private const val OG_TYPE = "og:type"
    }
}
