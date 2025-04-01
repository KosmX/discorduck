package dev.kosmx.discorducky.bot.modules

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import dev.kosmx.discorducky.VikBotHandler
import dev.kosmx.discorducky.eliza.ElizaWrapper
import io.github.populus_omnibus.vikbot.api.EventResult
import io.github.populus_omnibus.vikbot.api.annotations.Module
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

object ElizaReply {
    val chatAgents = CacheBuilder.newBuilder().apply {
        expireAfterAccess(3.hours.toJavaDuration())
    }.removalListener<Long, ElizaWrapper> { (_, value) -> value.close() }.build(object : CacheLoader<Long, ElizaWrapper>() {
        override fun load(key: Long): ElizaWrapper {
            return ElizaWrapper()
        }

    })


    @Module
    fun activate(bot: VikBotHandler) {

        bot.messageReceivedEvent[0] = { event ->
            if (!event.isFromGuild || (event.message.referencedMessage?.author?.isBot == true) || event.message.mentions.usersBag.any { user -> user.isBot }) {

                val nextLine = chatAgents[event.channel.idLong].transform(event.message.contentDisplay)

                event.message.reply(nextLine).queue()

                EventResult.CONSUME
            } else EventResult.PASS
        }

    }
}