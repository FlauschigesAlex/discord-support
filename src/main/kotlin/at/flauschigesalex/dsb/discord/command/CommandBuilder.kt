package at.flauschigesalex.dsb.discord.command

import at.flauschigesalex.dsb.JDA
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.RestAction

@OptIn(CommandInternal::class)
@Suppress("DEPRECATION", "unused")
class CommandBuilder(val name: String, block: CommandBuilder.() -> Unit) {
    @CommandInternal companion object {
        internal val commands = mutableListOf<CommandBuilder>()
        private var isRegistered = false
        fun registerAll(jda: JDA): RestAction<*> {
            if (isRegistered) return jda.updateCommands()
            isRegistered = true
            
            val commands = commands.toMutableList()
            if (commands.isEmpty()) return jda.updateCommands()
            
           val actions = mutableListOf(
               *commands.map { it.asRestAction() }.toTypedArray(),
               jda.updateCommands()
           )
            println(actions.size)
            jda.addEventListener(CommandListener)
            return RestAction.allOf(actions)
        }
    }
    
    var description: String = name
    var context: InteractionContextType = InteractionContextType.GUILD
    
    private var permissions = emptyList<Permission>()
    fun permission(permission: Permission, vararg permissions: Permission) {
        this.permissions = mutableListOf(permission, *permissions)
    }

    @Deprecated("") @CommandInternal
    val options = mutableListOf<CommandOption>()
    fun option(name: String, type: OptionType, block: CommandOption.() -> Unit) {
        options += CommandOption(name, type, block)
    }
    
    @Deprecated("") @CommandInternal
    var executor: CommandInvocation = { it.deferReply(true).queue() }
    fun execute(invocation: CommandInvocation) {
        this.executor = invocation
    }
    
    init {
        block(this)
        commands.add(this)
    }

    @Deprecated("") @CommandInternal
    fun asRestAction(): RestAction<*> {
        var action = JDA.upsertCommand(name, description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions))
            .setContexts(context)
        
        options.forEach { arg ->
            action = action.addOption(arg.type, arg.name, arg.description, arg.required, arg.autoComplete)
        }
        
        return action
    }
    
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = other is CommandBuilder && name.equals(other.name, true)
}