package at.flauschigesalex.dsb.discord.general

import at.flauschigesalex.dsb.JDA
import at.flauschigesalex.dsb.discord.command.CommandBuilder
import at.flauschigesalex.dsb.discord.command.CommandInternal
import at.flauschigesalex.dsb.discord.utils.queue

object Commands {
    init {
        CommandBuilder("sippi") {
            this.execute {
                it.reply("hallo welt").queue(true)
            }
        }

        @OptIn(CommandInternal::class)
        CommandBuilder.registerAll(JDA).queue()
    }
}