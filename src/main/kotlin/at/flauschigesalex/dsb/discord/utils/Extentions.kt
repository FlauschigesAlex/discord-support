package at.flauschigesalex.dsb.discord.utils

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IPermissionHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

val IPermissionHolder.isAdmin: Boolean
    get() = this.hasPermission(Permission.ADMINISTRATOR)

val Role.members: List<Member>
    get() = this.guild.members.filter { this in it.roles }

fun ReplyCallbackAction.queue(ephemeral: Boolean, success: (InteractionHook) -> Unit = {}) =
    this.setEphemeral(ephemeral).queue(success)