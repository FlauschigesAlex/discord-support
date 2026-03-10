package at.flauschigesalex.dsb.discord.utils

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

val Member.isAdmin: Boolean
    get() = this.hasPermission(Permission.ADMINISTRATOR)