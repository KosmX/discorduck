package dev.kosmx.discorducky

import io.github.populus_omnibus.vikbot.api.Event
import io.github.populus_omnibus.vikbot.api.commands.SlashCommand
import io.github.populus_omnibus.vikbot.api.commands.SlashOptionType
import io.github.populus_omnibus.vikbot.api.commands.operator
import io.github.populus_omnibus.vikbot.api.interactions.IdentifiableInteractionHandler
import io.github.populus_omnibus.vikbot.api.interactions.IdentifiableList
import io.github.populus_omnibus.vikbot.api.interactions.invoke
import io.github.populus_omnibus.vikbot.api.invoke
import dev.kosmx.discorducky.bot.BotConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.slf4j.kotlin.error
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import java.util.*

object VikBotHandler : EventListener {
    private val logger by getLogger()


    val messageReceivedEvent = Event.simple<MessageReceivedEvent>()
    val messageUpdateEvent = Event.simple<MessageUpdateEvent>()
    val messageContextInteractionEvent = Event.simple<MessageContextInteractionEvent>()
    val messageDeleteEvent = Event.simple<MessageDeleteEvent>()
    
    val reactionEvent = Event.simple<GenericMessageReactionEvent>()
    val guildVoiceUpdateEvent = Event.simple<GuildVoiceUpdateEvent>()
    val guildMemberRoleAddEvent = Event.simple<GuildMemberRoleAddEvent>()
    val guildMemberRoleRemoveEvent = Event.simple<GuildMemberRoleRemoveEvent>()
    val guildMemberUpdateNicknameEvent = Event.simple<GuildMemberUpdateNicknameEvent>()

    val initEvent = mutableListOf<(JDA) -> Unit>().apply { add(::registerCommands) }
    val readyEvent = mutableListOf<(ReadyEvent) -> Unit>()
    val guildInitEvent = mutableListOf<(GuildReadyEvent) -> Unit>().apply{ add(::registerNonGlobalCommands) }

    val maintainEvent = mutableListOf<() -> Unit>()
    val shutdownEvent = mutableListOf<() -> Unit>()

    @Deprecated("replaced by globalCommands", ReplaceWith("this.globalCommands"))
    val commands : MutableList<SlashCommand>
        get() = globalCommands

    val globalCommands = mutableListOf<SlashCommand>()
    val serverCommands = mutableListOf<SlashCommand>()
    val ownerServerCommands = mutableListOf<SlashCommand>()

    val buttonEvents = IdentifiableList<IdentifiableInteractionHandler<ButtonInteractionEvent>>()
    val modalEvents = IdentifiableList<IdentifiableInteractionHandler<ModalInteractionEvent>>()
    val stringSelectEvents = IdentifiableList<IdentifiableInteractionHandler<StringSelectInteractionEvent>>()
    val entitySelectEvents = IdentifiableList<IdentifiableInteractionHandler<EntitySelectInteractionEvent>>()

    private lateinit var _jda : JDA
    val jda : JDA
        get() = _jda
    lateinit var config: BotConfig

    private val timer = Timer()

    private val commandMap: Map<String, SlashCommand> by lazy {
        (globalCommands + serverCommands + ownerServerCommands).associateBy { it.name }
    }

    init {

        globalCommands += SlashCommand("ping", "quick self test") {
            it.reply("pong\nclient latency: ${it.jda.gatewayPing}").setEphemeral(true).complete()
        }
    }


    fun start() {
        val client: JDA = JDABuilder.createDefault(config.token).apply {
            // configure here
            setActivity(Activity.playing(config.initActivity))
            disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS)
            enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
            addEventListeners(this@VikBotHandler)
            setEnableShutdownHook(false)
        }.build()
        _jda = client
        jda.awaitReady()
        initEvent.forEach { it(jda) }

        logger.info("Bot is ready")

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down")
            timer.cancel()
            shutdownEvent.forEach { it() }
            client.shutdown()
        })

        timer.schedule(object : TimerTask(){
            override fun run() {
                maintainEvent.forEach{ it.invoke() }
            }
        }, 1000, 1000)

        // Bot is on
        logger.info("Bot is online")
    }

    override fun onEvent(event: GenericEvent) {
        logger.info { "Handling event type ${event::class.simpleName}" }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (event) {
                    is MessageReceivedEvent -> messageReceivedEvent(event)
                    is MessageUpdateEvent -> messageUpdateEvent(event)
                    is MessageContextInteractionEvent -> messageContextInteractionEvent(event)
                    is MessageDeleteEvent -> messageDeleteEvent(event)
                    
                    is GuildVoiceUpdateEvent -> guildVoiceUpdateEvent(event)
                    is ReadyEvent -> readyEvent.forEach { subscriber -> subscriber(event) }
                    is GuildReadyEvent -> guildInitEvent.forEach { subscriber -> subscriber(event) }
                    is GenericMessageReactionEvent -> reactionEvent(event)
                    is SlashCommandInteractionEvent -> {
                        commandMap[event.name]?.bindAndInvoke(event)
                            ?: logger.error { "executed command was not found: ${event.name}" }
                    }
                    is GuildMemberRoleAddEvent -> guildMemberRoleAddEvent(event)
                    is GuildMemberRoleRemoveEvent -> guildMemberRoleRemoveEvent(event)
                    is GuildMemberUpdateNicknameEvent -> guildMemberUpdateNicknameEvent(event)

                    is ButtonInteractionEvent -> buttonEvents(event.button.id, event, "button")
                    is ModalInteractionEvent -> modalEvents(event.modalId, event, "modal")
                    is StringSelectInteractionEvent -> stringSelectEvents(event.componentId, event, "stringSelect")
                    is EntitySelectInteractionEvent -> entitySelectEvents(event.componentId, event, "entitySelect")
                    is CommandAutoCompleteInteractionEvent -> commandMap[event.name]?.autoCompleteAction(event)
                        ?: run { event.replyChoiceStrings("error").complete() }
                }
            } catch (e: Throwable) {
                logger.error(e) { "Error while handling $event: ${e.message}" }
            }
        }
    }

    private fun registerCommands(jda: JDA) {
        jda.updateCommands().addCommands(
            globalCommands.map {
                Commands.slash(it.name, it.description).apply {
                    it.configure(this)
                }
            }
        ).queue()
    }

    private fun registerNonGlobalCommands(event: GuildReadyEvent) {
        val request = event.guild.updateCommands()

        logger.info { "Registering server commands on ${event.guild}" }
        request.addCommands(
            serverCommands.map {
                Commands.slash(it.name, it.description).apply {
                    it.configure(this)
                }
            }
        )
        request.complete()
    }


}