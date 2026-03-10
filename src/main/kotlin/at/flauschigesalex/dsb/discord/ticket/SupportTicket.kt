@file:OptIn(TicketInternal::class)

package at.flauschigesalex.dsb.discord.ticket

import at.flauschigesalex.dsb.JDA
import at.flauschigesalex.dsb.data.CategoryFlag
import at.flauschigesalex.dsb.data.MessageObject
import at.flauschigesalex.dsb.data.RoleFlag
import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.discord.utils.isAdmin
import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.dsb.utils.locale
import at.flauschigesalex.dsb.utils.replyLocalized
import at.flauschigesalex.dsb.utils.translate
import at.flauschigesalex.lib.base.file.JsonManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.w3c.dom.html.HTMLIsIndexElement
import java.lang.foreign.ValueLayout
import java.time.Instant
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_EXPRESSION")
class SupportTicket(val category: SupportCategory, private val json: JsonManager) : Comparable<SupportTicket>, Serializable() {
    
    companion object {
        @TicketInternal
        internal val entries = mutableSetOf<SupportTicket>()
        
        val READ_ONLY_PERMISSION = setOf(
            Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY
        ) to Permission.entries
        
        val open: Set<SupportTicket>
            get() = this.state(TicketState.OPEN)
        
        fun state(state: TicketState, vararg other: TicketState): Set<SupportTicket> {
            val require = other.toMutableSet().apply { 
                this.add(state)
            }
            
            return entries.filter { it.state in require }.toSet()
        }
        fun find(supplier: (SupportTicket) -> Boolean) = entries.find(supplier)
        fun filter(supplier: (SupportTicket) -> Boolean) = entries.filter(supplier).toSet()
        
        operator fun invoke(category: SupportCategory, member: Member, channel: TextChannel): SupportTicket {
            val json = JsonManager(
                "ids.members" to listOf(member.idLong),
                "ids.channel" to channel.idLong,
            )
            
            return SupportTicket(category, json)
        }
        
        val scheduler: ScheduledFuture<*> = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            this.entries.toSet().forEach { ticket ->
                val state = ticket.state
                val channel = ticket.channel ?: return@forEach ticket.archive(null)

                val now = Instant.now()
                val category = ticket.category
                val autoCloseDuration = category.autoCloseTime
                
                if (autoCloseDuration != null && state == TicketState.OPEN) {
                    val latestInteraction = ticket.latestInteraction
                    
                    val expireIn = latestInteraction.plus(autoCloseDuration)
                    if (expireIn.isBefore(now)) {
                        ticket.close(null, ticket.channel!!, null)
                    }
                    return@forEach
                }
                
                if (state == TicketState.CLOSED) {
                    val archiveTimestamp = (ticket.closeAction?.timestamp ?: ticket.latestInteraction)
                        .plus(category.autoArchiveTime)
                    
                    if (now.isAfter(archiveTimestamp)) 
                        ticket.archive(channel)
                    return@forEach
                }
                
                if (state == TicketState.ARCHIVED) {
                    val archiveTimestamp = (ticket.closeAction?.timestamp ?: ticket.latestInteraction)
                        .plus(category.archiveTicketDuration)
                    
                    if (now.isAfter(archiveTimestamp))
                        entries.remove(ticket)
                    return@forEach
                }
            }
        }, 0, 1, TimeUnit.SECONDS)
        
        init {
            SupportHandler // LOAD TICKETS
        }
    }
    
    private val guild: Guild
        get() = category.category.guild
    
    val memberIds: List<Long> = json.getLongList("ids.members")
    val members: List<Member>
        get() = guild.members.filter { memberIds.contains(it.idLong) }
    
    var state: TicketState
        get() = json.getString("state")?.let { TicketState.valueOf(it) } ?: run {
            state = TicketState.OPEN
            TicketState.OPEN
        }
        private set(value) = Unit.also { json["state"] = value.name }
    
    var closeAction: CloseAction?
        get() = json.getJson("close")?.let { CloseAction(it) }
        set(value) = Unit.also { json["close"] = value?.toJson() }
    
    internal var latestInteraction: Instant
        get() = Instant.ofEpochMilli(json.getLong("latestInteraction") ?: System.currentTimeMillis())
        private set(value) = Unit.also { json["latestInteraction"] = value.toEpochMilli() }
    
    val channelId: Long = json.getLong("ids.channel")!!
    val channel: TextChannel?
        get() = guild.getTextChannelById(channelId)
    
    var messages: Set<MessageObject>
        get() = json.getJsonList("messages").map { MessageObject(it) }.sortedBy { it.created }.toSet()
        private set(value) = Unit.also { json["messages"] = value.sortedBy { it.created }.map { it.toJson() } }

    init {
        entries.add(this)
        this.updateConfig()
    }

    fun consumeMessage(channel: TextChannel, message: Message) {
        latestInteraction = Instant.now()
        
        val obj = MessageObject(message)
        messages -= obj
        messages += obj
        
        this.updateConfig()
    }
    
    fun close(event: ButtonInteractionEvent?, channel: TextChannel, member: Member?) {
        if (state != TicketState.OPEN) return
        
        state = TicketState.CLOSED
        closeAction = CloseAction(member)

        val now = closeAction!!.timestamp.plus(category.autoArchiveTime)
        val message = (category.messages.closed ?: "${channel.locale.translate("ticket.close.info")}\n${channel.locale.translate("ticket.archive.info")}")
            .replace("{closedBy}", member?.asMention ?: JDA.selfUser.asMention, true)
            .replace("{member}", this.members.joinToString(" ") { it.asMention }, true)
            .replace("{time}", "<t:${now.epochSecond}:R>", true)
        
        // TODO ADD DOWNLOADABLE TRANSCRIPT
        event?.deferReply(true)?.queue {
            it.deleteOriginal().queue()
        }
        event?.message?.delete()?.queue()
        channel.manager.apply { 
            memberIds.forEach { memberId ->
                this.putMemberPermissionOverride(memberId, READ_ONLY_PERMISSION.first, READ_ONLY_PERMISSION.second)
            }
            category.roleDataEntries.forEach { data -> 
                this.putRolePermissionOverride(data.id, READ_ONLY_PERMISSION.first, READ_ONLY_PERMISSION.second)
            }
        }.queue {
            channel.sendMessage(message).queue()
        }
        this.updateConfig()
    }
    
    internal fun archive(channel: TextChannel?) {
        state = TicketState.ARCHIVED
        if (closeAction == null)
            closeAction = CloseAction(null)

        channel?.delete()?.queue()
        this.updateConfig()
    }
    
    fun isFlag(flag: CategoryFlag) = category.flags.contains(flag)
    
    private fun updateConfig() {
        SupportHandler.save(true)
    }

    override fun toJson(): JsonManager = this.json.clone()

    override fun compareTo(other: SupportTicket): Int = compareBy<SupportTicket> { it.state }
        .thenBy { it.latestInteraction }
        .compare(this, other)
}

enum class TicketState {
    OPEN,
    CLOSED,
    ARCHIVED,
}

class CloseAction(private val json: JsonManager): Serializable() {
    companion object {
        operator fun invoke(member: Member?): CloseAction = CloseAction(JsonManager(
            "_id" to member?.idLong,
            "timestamp" to System.currentTimeMillis(),
        ))
    }
    
    val memberId: Long? = json.getLong("_id")
    val timestamp: Instant = Instant.ofEpochMilli(json.getLong("timestamp") ?: System.currentTimeMillis())
    
    override fun toJson(): JsonManager = this.json.clone()
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class TicketInternal

enum class TicketButtonAction(private val consumer: SupportTicket.(event: ButtonInteractionEvent, initiator: Member, channel: TextChannel) -> Unit) {
    MANAGE({ event, initiator, _ ->
        val locale = event.userLocale.toLocale()

        val canCloseMember = !this.category.isFlag(CategoryFlag.MEMBER_CANT_CLOSE) && this.memberIds.first() == initiator.idLong
        val canCloseRole = initiator.idLong !in this.memberIds && initiator.roles
            .mapNotNull { role -> this.category.roleDataEntries.find { it.id == role.idLong } }
            .any { it.isFlag(RoleFlag.CLOSE_TICKET) }
        
        val canClose = initiator.isAdmin || canCloseMember || canCloseRole
        
        event.replyLocalized("ticket.manage", true).addComponents(ActionRow.of(
            Button.danger(CLOSE.id, locale.translate("ticket.close.splash")).withDisabled(canClose.not()),
        )).queue()
    }),
    CLOSE({ event, initiator, channel ->
        this.close(event, channel, initiator)
    }),
    // TRANSCRIPT({ event, initiator, channel -> }),
    ;
    
    val id = "dsb_${name.lowercase()}"
    
    companion object {
        fun find(id: String) = entries.find { id.equals(it.id, true) }
    }
    
    operator fun invoke(ticket: SupportTicket, event: ButtonInteractionEvent, initiator: Member, channel: TextChannel) =
        consumer(ticket, event, initiator, channel)
    
}