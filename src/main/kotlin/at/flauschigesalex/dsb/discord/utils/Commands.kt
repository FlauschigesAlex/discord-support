package at.flauschigesalex.dsb.discord.utils

import at.flauschigesalex.dsb.configuration.config
import at.flauschigesalex.lib.discord.command.CommandBuilder
import at.flauschigesalex.lib.discord.command.CommandOption
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.interactions.commands.OptionType

internal object Commands {
    
    init {
        CommandBuilder("setup-channel") {
            
            this.addOption(CommandOption(OptionType.STRING, "id"))
            this.addOption(CommandOption(OptionType.CHANNEL, "channel"))
            
            this.execute { event ->

                val id = event.getOption("id")?.asString ?: run {
                    val config = event.guild?.config ?: return@execute
                    val channel = config.channel ?: run { 
                        
                        return@execute
                    }
                    
                    
                    return@execute
                }
                
                val channel = event.getOption("channel")?.asChannel ?: event.channel
                if (channel !is GuildMessageChannel) {

                    return@execute
                }

                val categories = event.guild?.config?.supportCategories.orEmpty()
                val category = categories.find { it.id.equals(id, true) } ?: run {
                    
                    return@execute
                }

                channel.sendMessage(category.description).addComponents(ActionRow.of(
                    Button.primary("dsb-${category.id}", category.name)
                )).queue()
            }
        }
    }
}