package at.flauschigesalex.dsb.discord.ticket

import at.flauschigesalex.dsb.data.supportCategories
import at.flauschigesalex.lib.discord.listener.DiscordListener
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent

@Suppress("unused")
private class TicketListener : DiscordListener() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        val guild = event.guild ?: return
        val button = event.button
        val id = button.customId ?: return
        val channel = event.channel as? TextChannel ?: return
        
        if (!id.startsWith("dsb"))
            return

        SupportTicket.ButtonAction.find(id)?.run {
            val ticket = guild.tickets.find{ it.state == SupportTicket.State.OPEN && it.channelId == channel.idLong } ?: return
            return this(ticket, event, member, channel)
        }
        
        val categoryId = id.substringAfter("dsb-")
        val category = guild.supportCategories.find { it.id == categoryId } ?: return
        category.consumeEvent(event, member, guild)
    }
    
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.channel as? TextChannel ?: return
        val guild = event.guild

        val ticket = guild.tickets.find{ it.state == SupportTicket.State.OPEN && it.channelId == channel.idLong } ?: return
        ticket.consumeMessage(channel, event.message)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        val channel = event.channel as? TextChannel ?: return
        val guild = event.guild
        
        val ticket = guild.tickets.find{ it.state == SupportTicket.State.OPEN && it.channelId == channel.idLong } ?: return
        ticket.consumeMessage(channel, event.message)
    }
}