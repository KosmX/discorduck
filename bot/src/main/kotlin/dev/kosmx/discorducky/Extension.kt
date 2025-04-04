package dev.kosmx.discorducky

import io.github.populus_omnibus.vikbot.api.MaintainEvent


operator fun VikBotHandler.plusAssign(event: MaintainEvent<*, *>) {
    maintainEvent += event.generateTimerEvent()
    shutdownEvent += event.generateShutdownEvent()
}
