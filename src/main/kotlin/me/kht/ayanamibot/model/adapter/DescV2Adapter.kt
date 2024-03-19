package me.kht.ayanamibot.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Suppress("unused")
class DescV2Adapter {

    @FromJson
    @DescV2
    fun fromJson(desc: List<DescV2Json>): List<String> {
        return desc.map { it.rawText }
    }

    @ToJson
    fun toJson(@DescV2 value: List<String>): List<DescV2Json> {
        return value.map { DescV2Json(it) }
    }

    data class DescV2Json(
        @Json(name = "raw_text")
        val rawText: String
    )
}

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DescV2