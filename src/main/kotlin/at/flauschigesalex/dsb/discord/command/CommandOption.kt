package at.flauschigesalex.dsb.discord.command

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class CommandOption(val name: String, val type: OptionType, block: CommandOption.() -> Unit) {
    
    var description: String = name
    var required: Boolean = false
    var autoComplete: Boolean = false
    
    init { block(this) }
}

data class CommandOptionEntry(val option: CommandOption, private val value: OptionMapping) {

    val asAttachmentOrNull: Message.Attachment? = runCatching { value.asAttachment }.getOrNull()
    val asBooleanOrNull: Boolean? = runCatching { value.asBoolean }.getOrNull()
    val asChannelOrNull: GuildChannelUnion? = runCatching { value.asChannel }.getOrNull()
    val asDoubleOrNull: Double? = runCatching { value.asDouble }.getOrNull()
    val asIntOrNull: Int? = runCatching { value.asInt }.getOrNull()
    val asLongOrNull: Long? = runCatching { value.asLong }.getOrNull()
    val asMemberOrNull: Member? = runCatching { value.asMember }.getOrNull()
    val asMentionableOrNull: IMentionable? = runCatching { value.asMentionable }.getOrNull()
    val asRoleOrNull: Role? = runCatching { value.asRole }.getOrNull()
    val asStringOrNull: String? = runCatching { value.asString }.getOrNull()
    val asUserOrNull: User? = runCatching { value.asUser }.getOrNull()
    
    val asBoolean: Boolean = asBooleanOrNull == true
    val asDouble: Double = asDoubleOrNull ?: 0.0
    val asInt: Int = asIntOrNull ?: 0
    val asLong: Long = asLongOrNull ?: 0
    val asString: String = asStringOrNull ?: runCatching { 
        value::class.java.getDeclaredField("data").also { 
            it.isAccessible = true
        }.get(this).toString()
    }.getOrNull().toString()
}
