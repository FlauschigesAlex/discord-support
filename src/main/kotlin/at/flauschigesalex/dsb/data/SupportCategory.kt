@file:Suppress("unused")

package at.flauschigesalex.dsb.data

import at.flauschigesalex.dsb.configuration.GuildConfiguration
import at.flauschigesalex.dsb.configuration.config
import at.flauschigesalex.dsb.discord.ticket.SupportTicket
import at.flauschigesalex.dsb.discord.ticket.tickets
import at.flauschigesalex.dsb.logger
import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.dsb.utils.replyLocalized
import at.flauschigesalex.lib.base.file.JsonManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import java.time.Duration
import kotlin.collections.emptySet
import kotlin.math.max

class SupportCategory private constructor(
    val parent: GuildConfiguration,
    private val json: JsonManager,
) : Serializable() {

    companion object {
        private const val VERSION = 1
        
        operator fun invoke(parent: GuildConfiguration, json: JsonManager): SupportCategory? {
            // REFUSE TO CREATE CATEGORY FOR NON EXISTING GUILD
            if (parent.guild == null) return null
            
            val id = json.getString("_id")
            if (id.isNullOrBlank()) {
                logger.error("$parent contains invalid support category (missing '_id'): $json")
                return null
            }
            if (id.length > 16) {
                logger.error("$parent contains invalid support category (too long '_id'): $json")
                return null
            }
            
            return SupportCategory(parent, json)
        }
    }
    
    var id: String = json.getString("_id")!!
        set(value) {
            json["_id"] = value
            field = value
        }
    var version = json.getInt("_version") ?: 0 // TODO
        private set(value) {
            json["_version"] = value
            field = value
        }
    
    var name: String = json.getString("name") ?: id
        set(value) {
            json["name"] = value
            field = value
        }

    var description: String = json.getString("description") ?: "This is the $name category."
        set(value) {
            json["description"] = value
            field = value
        }
    
    var flags: Set<CategoryFlag> = json.getStringList("flags").mapNotNull { flag -> CategoryFlag.entries.find { it.name.equals(flag, true) } }.toSet()
        set(value) {
            json["flags"] = value.map { it.name }
            field = value
        }
    fun isFlag(flag: CategoryFlag) = flags.contains(flag)
    
    val messages: SupportMessages = SupportMessages(json)
    
    var category: Category? = json.getLong("category")?.let { parent.guild?.getCategoryById(it) }
        set(value) {
            json["category"] = value?.idLong
            field = value
        }
    
    var ticketLimit: Int = max(json.getInt("limit.amount") ?: 1, 1)
        set(value) {
            json["limit.amount"] = max(value, 1)
            field = value
        }
    
    var autoArchiveTime: Duration = json.getLong("durations.close.archive")?.let { Duration.ofSeconds(it) } ?: Duration.ofSeconds(6)
        set(value) {
            json["durations.close.archive"] = max(value.toSeconds(), 6)
            field = value
        }
    
    var archiveTicketDuration: Duration? = json.getLong("durations.archive")?.let { Duration.ofDays(it) } ?: Duration.ofDays(30)
        set(value) {
            json["durations.archive"] = value?.toDays()?.let { max(it, -1) }
            field = value
        }
    
    var autoCloseTime: Duration? = json.getLong("durations.close.auto")?.let { Duration.ofSeconds(max(it, 30)) }
        set(value) {
            json["durations.close.auto"] = value?.toSeconds()?.let { max(it, 30L) }
            field = value
        }
    
    var memberPermissions: MemberData = MemberData(json.getJson("data.member") ?: JsonManager())
        set(value) {
            json["data.member"] = value.toJson()
            field = value
        }
    
    var roleDataEntries: List<RoleData> = json.getJsonList("data.roles").mapNotNull { RoleData(it) }
        set(value) {
            json["data.roles"] = value.map { it.toJson() }
            field = value
        }
    
    init {
        logger.info("Enabled support category: $id ($name)")
    }
    
    fun consumeEvent(event: GenericComponentInteractionCreateEvent, member: Member, guild: Guild) {
        if (ticketLimit > 0) {
            val tickets = guild.tickets.filter { it.category == this && it.memberIds.contains(member.idLong) && it.state == SupportTicket.State.OPEN }
            if (tickets.size >= ticketLimit) {
                event.replyLocalized("discord.ticket.limit", ticketLimit, name).setEphemeral(true).queue()
                return
            }
        }
        
        this.createTicket(member, guild) { channel ->
            val reply = this@SupportCategory.messages.created
                ?.replace("{member}", member.asMention, true)
                ?.replace("{id}", id, true)
                ?.replace("{name}", name, true)
                ?.replace("{ticket}", channel.asMention, true)
            
            if (reply != null) event.deferReply(true).setContent(reply).queue()
            else event.deferReply(true).queue {
                it.deleteOriginal().queue()
            }
        }
    }
    
    internal fun createTicket(member: Member, guild: Guild, consumer: SupportTicket.(TextChannel) -> Unit) {
        val memberPermissions = memberPermissions

        var action = guild.createTextChannel("${this.id}-${member.user.name}", category)
            .addMemberPermissionOverride(member.idLong, memberPermissions.allowed, memberPermissions.denied)
            .addRolePermissionOverride(guild.publicRole.idLong, emptyList(), mutableListOf(
                Permission.VIEW_CHANNEL
            ))
        
        roleDataEntries.forEach { 
            action = action.addRolePermissionOverride(it.id, it.permissions.allowed, it.permissions.denied)
        }
        
        action.queue { channel ->
            val roleEntries = roleDataEntries
                .filter { it.flags.contains(RoleData.RoleFlag.IS_MENTION) }
                .mapNotNull { guild.getRoleById(it.id) }
            val roleMention = roleEntries.joinToString(" ") { role -> role.asMention }
            val message = messages.initial ?: roleMention
            
            if (message.isEmpty())
                return@queue
            
            val formattedMessage = message
                .replace("{member}", member.asMention, true)
                .replace("{roles}", roleMention, true)

            val ticket = SupportTicket(this, member, channel)
            ticket.manager.updateTicket(ticket)
            
            channel.sendMessage(formattedMessage).addComponents(ActionRow.of(
                Button.secondary(SupportTicket.ButtonAction.MANAGE.id, Emoji.fromUnicode("U+2699")),
            )).queue {
                consumer(ticket, channel)
            }
        }
    }

    override fun toJson(): JsonManager = json.clone()
    override fun hashCode(): Int = this.id.hashCode()
    override fun equals(other: Any?): Boolean = other is SupportCategory && this.id == other.id

    class SupportMessages(private val json: JsonManager) {

        var initial: String?
            get() = json.getString("messages.initial")
            set(value) = Unit.also { json["messages.initial"] = value }

        var created: String?
            get() = json.getString("messages.created")
            set(value) = Unit.also { json["messages.created"] = value }

        var closed: String?
            get() = json.getString("messages.closed")
            set(value) = Unit.also { json["messages.closed"] = value }
    }
    enum class CategoryFlag {
        MEMBER_CANT_CLOSE,
        ;
    }
}

val Guild.supportCategoriesOrNull: Set<SupportCategory>?
    get() = config?.supportCategories
val Guild.supportCategories: Set<SupportCategory>
    get() = supportCategoriesOrNull ?: emptySet()