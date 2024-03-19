package me.kht.ayanamibot.plugin

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotType
import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileWriter
import java.net.URLEncoder
import java.util.*
import kotlin.io.path.Path

private const val USAGE = """
    $>< <word>[#<language>[F]]
    Language is optional, default to English.
    If F is present, force lookup even if cache exists.
    
    $><? <word>
    List all available languages for the given word.
    """

private const val DESC = """
    Look up a word in Wiktionary.
    """

private const val TAG = "Wiktionary"

@Component
class WiktionaryPlugin(
    groupFeatures: HashMap<Long, Set<String>>,
    private val httpClient: OkHttpClient,
    private val browser: Browser
) : BasePlugin(groupFeatures, USAGE, DESC, TAG) {

    private val commandRegex = Regex("""^\$>< ([^#]*)(#([a-zA-Z]*)(F?))?""")
    private val listCommandRegex = Regex("""^\$><\? (.+)""")

    private val logger = KotlinLogging.logger(TAG)

    private val languageMaps = mapOf(
        "en" to "English",
        "zh" to "Chinese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "fr" to "French",
        "de" to "German",
        "es" to "Spanish",
        "it" to "Italian",
        "la" to "Latin",
        "ru" to "Russian",
        "ar" to "Arabic",
        "fa" to "Persian",
    )

    private fun match(message: String): Boolean {
        return commandRegex.matchEntire(message) != null
    }

    private fun lookup(message: String): Result<String> {
        val match = commandRegex.matchEntire(message)!!
        val word = match.groupValues[1]
        val languageRaw = match.groupValues[3].ifEmpty { "English" }

        // if language starts with lowercase letter, map it to language name
        val language = languageMaps[languageRaw] ?: languageRaw

        val force = match.groupValues[4].isNotEmpty()

        logger.info { "Looking up $word in $language" }

        val imgFile = "/tmp/wiktionary_${word}_$language.png"
        if (!force && File(imgFile).exists()) {
            logger.info { "Cache hit for $word in $language" }
            return Result.success(imgFile)
        }

        if (force) {
            logger.info { "Force lookup for $word in $language" }
        }

        val wordEncoded = URLEncoder.encode(word, "UTF-8")
        val url = "https://en.wiktionary.org/wiki/$wordEncoded"

        val htmlRaw = httpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute().body?.string() ?: return Result.failure(Exception("Failed to fetch data from Wiktionary."))

        logger.info { "Fetched data from Wiktionary" }

        val document = Jsoup.parse(htmlRaw)
        val head = document.head()
        val body = document.body()

        // find h2 > span#$lang
        val langSpan = body.selectFirst("h2 > span#$language")
        val langHeader = langSpan?.parent()
            ?: return Result.failure(Exception("Failed to find language section in Wiktionary."))

        val contents = mutableListOf(langHeader)

        while (true) {
            val next = contents.last().nextElementSibling()
            if (next == null || next.tagName() == "h2") {
                break
            }
            contents.add(next)
        }

        logger.info { "Building html" }
        // build head and contents to html
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<html><head>")
        htmlBuilder.append(head.html())
        htmlBuilder.append("</head><body>")
        contents.forEach { htmlBuilder.append(it.outerHtml()) }
        htmlBuilder.append("</body></html>")
        val html = htmlBuilder.toString()

        val id = UUID.randomUUID().toString()
        val htmlFile = "/tmp/wiktionary-$id.html"

        // write to html file
        val htmlWriter = FileWriter(htmlFile)
        htmlWriter.write(html)
        htmlWriter.close()

        logger.info { "Render to image" }
        // render to image

        browser.newPage().run {
            setContent(html)
            screenshot(Page.ScreenshotOptions().apply {
                path = Path(imgFile)
                fullPage = true
                type = ScreenshotType.JPEG
                quality = 100
            })
        }

        File(htmlFile).delete()

        return Result.success(imgFile)
    }

    private fun listLanguages(word: String): Result<List<String>> {
        val url = "https://en.wiktionary.org/wiki/$word"
        val htmlRaw =
            httpClient.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return Result.failure(
                Exception("Failed to fetch data from Wiktionary.")
            )
        val jsoup = Jsoup.parse(htmlRaw)

        // find all h2 > span
        val numberSpans = jsoup.select("ul > li > a > .tocnumber").filter { element: Element ->
            !element.text().contains(".")
        }
        val languages = numberSpans.map { element: Element ->
            element.nextElementSibling()!!.text()
        }
        return Result.success(languages)
    }

    override fun onAnyMessage(bot: Bot?, event: AnyMessageEvent?): Int {
        bot ?: return MESSAGE_IGNORE
        event ?: return MESSAGE_IGNORE

        shouldWork(event) || return MESSAGE_IGNORE

        val message = event.rawMessage.trim()
        if (listCommandRegex.matchEntire(message) != null) {
            val word = listCommandRegex.matchEntire(event.message)!!.groupValues[1]
            val result = listLanguages(word)
            if (result.isFailure) {
                bot.sendMsg(event, "Failed to fetch data from Wiktionary.", false)
                return MESSAGE_IGNORE
            }
            val languages = result.getOrNull()!!
            val msg = MsgUtils.builder()
                .text("Available languages for $word: ${languages.joinToString(", ")}")
                .build()
            bot.sendMsg(event, msg, false)
            return MESSAGE_IGNORE
        }

        match(message) || return MESSAGE_IGNORE

        val result = lookup(message)
        if (result.isFailure) {
            bot.sendMsg(event, "Failed to fetch data from Wiktionary.", false)
            return MESSAGE_IGNORE
        }

        val imgFile = result.getOrNull()!!

        val msg = MsgUtils.builder()
            .img("file://$imgFile")
            .build()

        bot.sendMsg(event, msg, false)

        return MESSAGE_IGNORE
    }
}