package me.kht.ayanamibot

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import com.squareup.moshi.Moshi
import com.squareup.moshi.addAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.kht.ayanamibot.model.adapter.DescV2Adapter
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.Proxy

typealias GroupFeatures = HashMap<Long, Set<String>>

@Component
class AppData {

    private val groupFeatures = hashMapOf<Long, Set<String>>()

    @Bean
    fun groupFeatures() = groupFeatures

    @Bean
    fun httpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (System.getenv("PROD") != null) {
            builder.proxy(Proxy.NO_PROXY)
        } else {
            builder.proxy(Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 7890)))
        }

        return builder.build()
    }

    @Bean
    fun pluginList() = listOf(
        "Echo",
        "Wiktionary",
        "RustDocs",
        "Bilibili"
    )

    @Bean
    fun browser(): Browser {
        val playWright = Playwright.create()
        val browser = playWright.chromium().launch()
        return browser
    }

    @Bean
    fun jsonEncoder(): Moshi = Moshi.Builder()
        .add(DescV2Adapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()
}