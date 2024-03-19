package me.kht.ayanamibot.plugin

import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import me.kht.ayanamibot.GroupFeatures
import me.kht.ayanamibot.model.BilibiliVideoBasicInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component

private const val USAGE = """
    Any message containing a Bilibili video link will automatically trigger this.
"""

private const val DESC = """
    Automatically retrieve Bilibili video information.
"""

private const val TAG = "Bilibili"

@Component
class BilibiliPlugin(groupFeatures: GroupFeatures, private val httpClient: OkHttpClient, private val json: Moshi) :BasePlugin(groupFeatures, USAGE, DESC, TAG) {

    private val bilibiliRegex = Regex("""https://www.bilibili.com/video/BV(\w+)""", RegexOption.IGNORE_CASE)

    private val logger = KotlinLogging.logger(TAG)

    @OptIn(ExperimentalStdlibApi::class)
    override fun onAnyMessage(bot: Bot?, event: AnyMessageEvent?): Int {
        bot ?: return MESSAGE_IGNORE
        event ?: return MESSAGE_IGNORE

        shouldWork(event) || return MESSAGE_IGNORE

        val message = event.rawMessage

        // see if the message contains a bilibili link
        val matchEntries = bilibiliRegex.find(message) ?: return MESSAGE_IGNORE

        val bv = matchEntries.groupValues[1]

        val basicUrl = "https://api.bilibili.com/x/web-interface/view?bvid=BV$bv"

        logger.info { "Fetching Bilibili video info for $basicUrl" }

        val request = Request.Builder()
            .url(basicUrl)
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return MESSAGE_IGNORE

        val infoAdapter = json.adapter<BilibiliVideoBasicInfo>()
        val info = infoAdapter.fromJson(body) ?: return MESSAGE_IGNORE

        if (info.code != 0 || info.data == null) {
            bot.sendMsg(event, "Failed to fetch data from Bilibili. Code ${info.code}", false)
            return MESSAGE_BLOCK
        }

        val msg = MsgUtils.builder()
            .text("${info.data.title}\n")
            .text("${info.data.owner.name}\n")
            .text("${info.data.desc}\n")
            .img(info.data.pic)
            .text("评论: ${info.data.stat.reply} ")
            .text("播放: ${info.data.stat.view} ")
            .text("弹幕: ${info.data.stat.danmaku}\n")
            .text("点赞: ${info.data.stat.like} ")
            .text("投币: ${info.data.stat.coin} ")
            .text("收藏: ${info.data.stat.favorite}\n")
            .text("https://www.bilibili.com/video/BV$bv")

        bot.sendMsg(event, msg.build(), false)

        return MESSAGE_BLOCK
    }
}