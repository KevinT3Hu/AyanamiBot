package me.kht.ayanamibot.plugin

import com.mikuac.shiro.core.BotPlugin
import com.mikuac.shiro.dto.event.message.AnyMessageEvent

class BasePlugin(
    private val groupFeatures: HashMap<Long, Set<String>>,
    private val usage: String = "This is the base plugin",
    private val desc: String = "This is the base plugin",
    private val tag: String = "Base"
) : BotPlugin() {

    fun getUsage() = usage

    fun getDesc() = desc

    fun getTag() = tag

    fun enabled(groupId: Long): Boolean {
        return groupFeatures[groupId]?.contains(getTag()) ?: false
    }

    fun shouldWork(event: AnyMessageEvent): Boolean {
        val groupId: Long? = event.groupId
        return groupId == null || enabled(event.groupId)
    }
}