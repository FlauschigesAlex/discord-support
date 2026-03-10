package at.flauschigesalex.dsb.discord.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.RestAction

@CommandInternal
@Suppress("DEPRECATION")
object CommandListener : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.commandString
        val command = CommandBuilder.commands.find { it.name.equals(commandName, true) } ?: return
        
        val options = event.options.mapNotNull { option ->
            val iOption = command.options.find { it.name.equals(option.name, true) && it.type == option.type }
                ?: return@mapNotNull null
            
            return@mapNotNull CommandOptionEntry(iOption, option)
        }
        
        val any = command.executor.invoke(CommandData(
            guild = event.guild,
            locale = event.userLocale.toLocale(),
            member = event.member,
            options = options,
            user = event.user
        ), event)

        if (event.isAcknowledged)
            return
        
        if (any is RestAction<*>) {
            any.queue()
            return
        }
            
        event.deferReply().queue()
    }
}