package at.flauschigesalex.dsb.discord

import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.discord.DiscordBot.JDA
import at.flauschigesalex.dsb.utils.sendLocalizedMessage
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object SetupListener : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val member = event.member ?: return
        val guild = event.guild
        val message = event.message

        if (!member.hasPermission(Permission.ADMINISTRATOR)) return

        val content = message.contentRaw
        val args = content.split(" ")

        if (args[0] != JDA.selfUser.asMention) return

        if (args.size == 1)
            return event.channel.sendLocalizedMessage("system.functional", member.asMention)
                .setMessageReference(message)
                .queue()

        if (args.getOrNull(1).equals("setup", true)) {
            if (args.getOrNull(2).equals("message", true)) {
                val id = args.getOrNull(3) ?: return
                val category = SupportCategory.entries.find { it.id.equals(id, true) } ?: return
                
                event.channel.sendMessage(category.description).addComponents(ActionRow.of(
                    Button.primary("dsb-${category.id}", category.name)
                )).queue()
            }
        }
    }
}