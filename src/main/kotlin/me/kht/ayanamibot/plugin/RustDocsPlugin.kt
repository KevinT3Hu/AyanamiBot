package me.kht.ayanamibot.plugin

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotType
import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import me.kht.ayanamibot.GroupFeatures
import org.springframework.stereotype.Component
import kotlin.io.path.Path

private const val DESC = """
    Query Rust documentation.
"""

private const val USAGE = """
    $| <Path>[!@%*][#crate]?
    ! for Struct, @ for Trait, % for Enum, * for Function,
    #crate for crate. Will query std if not provided.
    
    Example: $| std::fs::File!
"""

@Component
class RustDocsPlugin(groupFeatures: GroupFeatures, private val browser: Browser): BasePlugin(groupFeatures, USAGE, DESC,"RustDocs") {

    private val commandRegex = Regex("""\$\| ([^!@%]+)([!@%*])(#([A-Za-z\-_]+))?""")

    private val logger = KotlinLogging.logger("RustDocs")

    override fun onAnyMessage(bot: Bot?, event: AnyMessageEvent?): Int {
        bot ?: return MESSAGE_IGNORE
        event ?: return MESSAGE_IGNORE

        shouldWork(event) || return MESSAGE_IGNORE

        val message = event.rawMessage

        val matchEntries = commandRegex.matchEntire(message)

        matchEntries ?: return MESSAGE_IGNORE

        val path = matchEntries.groupValues[1]
        val type = matchEntries.groupValues[2]
        val rustDocType = when (type) {
            "!" -> RustDocType.STRUCT
            "@" -> RustDocType.TRAIT
            "%" -> RustDocType.ENUM
            "*" -> RustDocType.FUNCTION
            else -> RustDocType.FUNCTION
        }

        val crate = matchEntries.groupValues[4].ifEmpty { "std" }

        val encodedPath = buildString {
            val pathSegments = path.split("::")
            val lastSeg = pathSegments.last()
            pathSegments.dropLast(1).forEach {
                append(it)
                append("/")
            }
            val lastSegPath = "${rustDocType.type}.${lastSeg}.html"
            append(lastSegPath)
        }

        val url = if (crate=="std") {
            "https://doc.rust-lang.org/$encodedPath"
        } else {
            "https://docs.rs/$crate/latest/$encodedPath"
        }

        logger.info { "URL: $url" }

        val imgFile = "/tmp/rustdoc-${crate}-${path}-${type}.jpg"

        browser.newPage().run {
            navigate(url)
            screenshot(Page.ScreenshotOptions().apply {
                this.path = Path(imgFile)
                fullPage = true
                this.type = ScreenshotType.JPEG
                quality = 100
            })
        }

        val msg = MsgUtils.builder()
            .img("file://$imgFile")
            .build()

        bot.sendMsg(event, msg, false)

        return MESSAGE_BLOCK
    }

    enum class RustDocType(val type: String) {
        STRUCT("struct"),
        TRAIT("trait"),
        ENUM("enum"),
        FUNCTION("fn"),
    }
}