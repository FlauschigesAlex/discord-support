package at.flauschigesalex.dsb.discord.command

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import java.util.Locale

typealias CommandInvocation = (CommandData.(SlashCommandInteraction) -> Any?)

data class CommandData(
    val guild: Guild?,
    val locale: Locale,
    val member: Member?,
    val options: List<CommandOptionEntry>,
    val user: User,
)