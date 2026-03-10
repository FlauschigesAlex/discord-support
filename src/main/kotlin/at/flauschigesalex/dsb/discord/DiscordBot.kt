package at.flauschigesalex.dsb.discord

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.dsb.discord.command.CommandInternal
import at.flauschigesalex.dsb.discord.command.CommandListener
import at.flauschigesalex.dsb.discord.general.Commands
import at.flauschigesalex.dsb.discord.general.LegacySetupListener
import at.flauschigesalex.dsb.discord.general.SetupListener
import at.flauschigesalex.dsb.discord.ticket.TicketListener
import at.flauschigesalex.lib.base.file.Environment
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

@Suppress("UNUSED_EXPRESSION")
object DiscordBot {
    
    val JDA: JDA
    
    init {
        val token = Environment["DISCORD_TOKEN"]
        JDA = JDABuilder.createDefault(token, GatewayIntent.entries)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build().awaitReady()

        JDA.addEventListener(LegacySetupListener)
        JDA.addEventListener(SetupListener)
        JDA.addEventListener(TicketListener)

        Configuration // LOAD CONFIG
        Commands // LOAD COMMANDS
    }
}