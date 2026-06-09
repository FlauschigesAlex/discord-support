package at.flauschigesalex.dsb.discord

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.dsb.discord.utils.Commands
import at.flauschigesalex.lib.base.file.Environment
import at.flauschigesalex.lib.discord.FlauschigeLibraryDiscord
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

        FlauschigeLibraryDiscord.init(JDA, DiscordBot::class.java)

        Configuration // LOAD CONFIG
        Commands // LOAD COMMANDS
    }
}