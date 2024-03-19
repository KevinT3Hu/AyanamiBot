package me.kht.ayanamibot.model

import com.squareup.moshi.Json
import me.kht.ayanamibot.model.adapter.DescV2

data class BilibiliVideoBasicInfo(
    val code: Int,
    val data : BasicInfoData? = null,
) {
    data class BasicInfoData(
        val bvid: String,
        val aid:Int,
        val tname: String,
        val copyright: Int,
        val pic: String,
        val title: String,
        val pubdate: Long,
        val ctime: Long,
        @Json(name = "desc_v2")
        @DescV2
        val desc: List<String>,
        val duration: Int,
        val owner: VideoOwner,
        val stat: Stat
    ) {
        data class VideoOwner(
            val mid: Long,
            val name: String,
            val face: String,
        )

        data class Stat(
            val view: Int,
            val danmaku: Int,
            val reply: Int,
            val favorite: Int,
            val coin: Int,
            val like: Int
        )
    }
}