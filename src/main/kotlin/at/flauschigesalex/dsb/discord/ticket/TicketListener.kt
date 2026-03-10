package at.flauschigesalex.dsb.discord.ticket

import at.flauschigesalex.dsb.data.SupportCategory
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object TicketListener : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        val guild = event.guild ?: return
        val button = event.button
        val id = button.customId ?: return
        val channel = event.channel as? TextChannel ?: return
        
        if (!id.startsWith("dsb"))
            return
        
        TicketButtonAction.find(id)?.run {
            val ticket = SupportTicket.open.find { it.channelId == channel.idLong } ?: return
            return this(ticket, event, member, channel)
        }
        
        val supportCategory = id.substringAfter("dsb-").let { SupportCategory.entries.find { entry -> entry.id.equals(it, true) } } ?: return
        supportCategory.consumeEvent(event, member, guild)
    }
    
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.channel as? TextChannel ?: return
            
        val ticket = SupportTicket.open.find { it.channelId == channel.idLong } ?: return
        ticket.consumeMessage(channel, event.message)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        val channel = event.channel as? TextChannel ?: return

        val ticket = SupportTicket.open.find { it.channelId == channel.idLong } ?: return
        ticket.consumeMessage(channel, event.message)
    }
}