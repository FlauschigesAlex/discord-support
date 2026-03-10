package at.flauschigesalex.dsb.discord.general

import at.flauschigesalex.dsb.configuration.config
import at.flauschigesalex.dsb.discord.utils.isAdmin
import at.flauschigesalex.dsb.discord.utils.members
import at.flauschigesalex.dsb.utils.sendLocalizedMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@OptIn(ExperimentalCoroutinesApi::class)
object SetupListener : ListenerAdapter() {

    override fun onGuildJoin(event: GuildJoinEvent) {
        val guild = event.guild
        val self = guild.selfMember
        if (!self.isAdmin) return
        
        val adminRoles = guild.roles.filter { it.isAdmin }.filter {
            it.members.toMutableList().apply {
                this -= self
            }.isNotEmpty()
        }.joinToString(" ") { it.asMention }

        val providedChannel = guild.config?.channel ?: guild.systemChannel
        val deferred = providedChannel?.let { CompletableDeferred(it) } ?: this.createSetupChannel(guild)
        deferred.invokeOnCompletion { 
            val channel = deferred.getCompleted()
            
            channel.sendLocalizedMessage("setup.welcome", self.asMention, adminRoles)
                .addComponents(ActionRow.of(
                    Button.link("https://github.com/FlauschigesAlex/discord-support/README.md", "Learn more")
                ))
                .queue()
        }
    }
    
    private fun createSetupChannel(guild: Guild): Deferred<TextChannel> {
        val deferred = CompletableDeferred<TextChannel>()

        guild.createTextChannel("setup")
            .addRolePermissionOverride(guild.publicRole.idLong, emptyList(), listOf(Permission.VIEW_CHANNEL))
            .queue { deferred.complete(it) }

        return deferred
    }
}