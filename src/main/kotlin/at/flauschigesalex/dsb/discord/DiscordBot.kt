package at.flauschigesalex.dsb.discord

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.dsb.discord.ticket.TicketListener
import at.flauschigesalex.dsb.logger
import at.flauschigesalex.lib.base.file.Environment
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

@Suppress("UNUSED_EXPRESSION")
object DiscordBot {
    
    val JDA: JDA
    
    init {
        val token = Configuration.discord.token ?: Environment["DISCORD_TOKEN"]?.let {
            logger.error("Using environment variable DISCORD_TOKEN is deprecated and marked for removal.")
            return@let it
        } ?: throw IllegalStateException("Bot token cannot be null!")
        
        JDA = JDABuilder.createDefault(token, GatewayIntent.entries)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build().awaitReady()
        
        JDA.addEventListener(SetupListener)
        JDA.addEventListener(TicketListener)
    }
}