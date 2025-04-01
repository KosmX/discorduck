package dev.kosmx.discorducky.bot

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File


@Serializable
data class BotConfig(
    val token: String,
    val botName: String = "Yamrizs",
    val initActivity: String = "quack",
    //var mailChannel: Long,
    val embedColor: Int = 0x03FCC2, //HEX VALUE
    val activeTimeZone: String = "UTC", // CET for Hungary
) {

    @Transient
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    companion object Lock

    @OptIn(ExperimentalSerializationApi::class, InternalCoroutinesApi::class)
    fun save() = synchronized(Lock) {
        File("bot.config.json").outputStream().use { output ->
            json.encodeToStream(this, output)
        }
    }
}