package dev.kosmx.discorducky.bot

import dev.kosmx.discorducky.VikBotHandler
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.*
import kotlin.time.Duration

fun Long.toUserTag() = "<@$this>"

fun Long.toRoleTag() = "<@$this>"

fun Long.toChannelTag() = "<#$this>"

private fun Number.padTime(length: Int = 2) = this.toString().padStart(length, '0')

fun String.chunkedMaxLength(maxSize: Int, separator: Char = '\n'): Sequence<String> = sequence {
    var index = 0
    while (index + maxSize < this@chunkedMaxLength.length) {
        val n = this@chunkedMaxLength.lastIndexOf(startIndex = maxSize, char = separator)
        yield(substring(index, n))
        index = n
    }
    yield(substring(index))
}



private val activeTimeZone by lazy { TimeZone.of(VikBotHandler.config.activeTimeZone) }

fun Instant.stringify(displaySeconds: Boolean = false): String {
    return this.toLocalDateTime(activeTimeZone).run {
        "${
            dayOfWeek.getDisplayName(
                TextStyle.SHORT,
                Locale.ENGLISH
            )
        }, " +
                "$year.${month.value.padTime()}.${dayOfMonth.padTime()}. " +
                "${hour.padTime()}:${minute.padTime()}${
                    if (displaySeconds) {
                        second.padTime()
                    } else ""
                }"
    }
}

fun java.time.Instant.stringify(displaySeconds: Boolean = false) = this.toKotlinInstant().stringify(displaySeconds)

fun OffsetDateTime.stringify(displaySeconds: Boolean = false) = this.toInstant().stringify(displaySeconds)

fun LocalDateTime.stringify(displaySeconds: Boolean = false) = this.toInstant(activeTimeZone).stringify(displaySeconds)

fun java.time.LocalDateTime.stringify(displaySeconds: Boolean = false) = this.toKotlinLocalDateTime().stringify(displaySeconds)


fun Duration.stringify(showZeroHours: Boolean = false): String {
    return this.toComponents { hours, minutes, seconds, _ ->
        (if (hours > 0 || showZeroHours) "${hours.padTime()}:" else "") +
                "${minutes.padTime()}:" +
                seconds.padTime()
    }
}