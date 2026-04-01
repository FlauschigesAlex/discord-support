@file:Suppress("UNUSED_EXPRESSION")

package at.flauschigesalex.dsb

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.discord.DiscordBot
import at.flauschigesalex.dsb.ktor.KtorApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    Configuration // LOAD CONFIG
    DiscordBot // ENABLE BOT
    SupportCategory // ENABLE CATEGORIES
    KtorApplication // ENABLE KTOR
}

val logger: Logger = LoggerFactory.getLogger("DSB")
val JDA: JDA get() = DiscordBot.JDA

private val asyncDispatcher = SupervisorJob() + Dispatchers.IO
fun scheduleAsync(block: suspend () -> Unit) {
    CoroutineScope(asyncDispatcher).launch {
        block()
    }
}