package at.flauschigesalex.dsb.data

import at.flauschigesalex.dsb.JDA
import at.flauschigesalex.dsb.discord.ticket.SupportTicket
import at.flauschigesalex.dsb.discord.ticket.TicketButtonAction.*
import at.flauschigesalex.dsb.logger
import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.lib.base.file.JsonManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.Duration
import kotlin.math.max

class SupportCategory internal constructor(
    private val json: JsonManager,
) : Serializable() {

    companion object {
        private val _entries = mutableSetOf<SupportCategory>()
        val entries: Set<SupportCategory>
            get() = _entries
        
        fun entries(entries: Set<SupportCategory>) {
            _entries.clear()
            _entries.addAll(entries)
        }
    }
    
    var id: String
        get() = json.getString("_id")!!
        set(value) = Unit.also { json["_id"] = value }
    
    var name: String
        get() = json.getString("name") ?: id
        set(value) = Unit.also { json["name"] = value }

    var description: String
        get() = "This is the support category."
        set(value) = Unit.also { json["description"] = value }
    
    var flags: Set<CategoryFlag>
        get() = json.getStringList("flags").mapNotNull { flag -> CategoryFlag.entries.find { it.name.equals(flag, true) } }.toSet()
        set(value) = Unit.also { json["flags"] = value.map { it.name } }
    fun isFlag(flag: CategoryFlag) = flags.contains(flag)
    
    val messages: SupportMessages = SupportMessages(json)
    
    var category: Category
        get() = json.getLong("category")?.let { JDA.getCategoryById(it) } ?: throw IllegalStateException("Category must exist in support: $json")
        set(value) = Unit.also { json["category"] = value.idLong }
    
    var ticketLimit: Int
        get() = max(json.getInt("limit.amount") ?: 1, 1)
        set(value) = Unit.also { json["limit.amount"] = max(value, 1) }
    
    var autoArchiveTime: Duration
        get() = json.getLong("durations.close.archive")?.let { Duration.ofSeconds(it) } ?: Duration.ofSeconds(6)
        set(value) = Unit.also { json["durations.close.archive"] = max(value.toSeconds(), 6) }
    
    var archiveTicketDuration: Duration?
        get() = json.getLong("durations.archive")?.let { Duration.ofDays(it) } ?: Duration.ofDays(30)
        set(value) = Unit.also { json["durations.archive"] = value?.toDays()?.let { max(it, -1) } }
    
    var autoCloseTime: Duration?
        get() = json.getLong("durations.close.auto")?.let { duration ->
            if (duration <= 0)
                return@let null
            return@let Duration.ofSeconds(duration)
        }
        set(value) = Unit.also { json["durations.close.auto"] = value?.toSeconds()?.let { max(it, 1L) } }
    
    var memberPermissions: MemberData
        get() {
            val json = json.getJson("data.member") ?: JsonManager()
            val permissionSet = MemberData(json)
            
            return permissionSet
        }
        set(value) = Unit.also { json["data.member"] = value.toJson() }
    
    var roleDataEntries: List<RoleData>
        get() = json.getJsonList("data.roles").mapNotNull {
            return@mapNotNull RoleData(it)
        }
        set(value) = Unit.also { json["data.roles"] = value.map { it.toJson() } }
    
    init {
        _entries.add(this)
        logger.info("Enabled support category: $id ($name)")
    }
    
    fun consumeEvent(event: ButtonInteractionEvent, member: Member, guild: Guild) {
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
    
    internal fun createTicket(member: Member, guild: Guild, consumer: SupportTicket.(TextChannel) -> Unit): Deferred<SupportTicket> {
        val memberPermissions = memberPermissions

        val action = category.createTextChannel("${this.id}-${member.user.name}")
            .addMemberPermissionOverride(member.idLong, memberPermissions.allowed, memberPermissions.denied)
        
        roleDataEntries.forEach { 
            action.addRolePermissionOverride(it.id, it.permissions.allowed, it.permissions.denied)
        }
        
        val deferred = CompletableDeferred<SupportTicket>()
        action.queue { channel ->
            val roleEntries = roleDataEntries
                .filter { it.flags.contains(RoleFlag.IS_MENTION) }
                .mapNotNull { category.guild.getRoleById(it.id) }
            val roleMention = roleEntries.joinToString(" ") { role -> role.asMention }
            val message = messages.initial ?: roleMention
            
            if (message.isEmpty())
                return@queue
            
            val formattedMessage = message
                .replace("{member}", member.asMention, true)
                .replace("{roles}", roleMention, true)

            val ticket = SupportTicket(this, member, channel)
            channel.sendMessage(formattedMessage).addComponents(ActionRow.of(
                Button.secondary(MANAGE.id, Emoji.fromUnicode("U+2699")),
            )).queue {
                deferred.complete(ticket)
                consumer(ticket, channel)
            }
        }
        
        return deferred
    }

    override fun toJson(): JsonManager = json.clone()

    override fun hashCode(): Int = this.id.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is SupportCategory) return false
        return this.id.equals(other.id, true)
    }
}

enum class CategoryFlag {
    MEMBER_CANT_CLOSE,
    ;
}

data class SupportMessages(private val json: JsonManager) {

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