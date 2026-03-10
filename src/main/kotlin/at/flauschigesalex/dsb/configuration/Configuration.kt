package at.flauschigesalex.dsb.configuration

import at.flauschigesalex.dsb.JDA
import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.scheduleAsync
import at.flauschigesalex.lib.base.file.FileManager
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.file.readJson
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

object Configuration {
    
    private val file: FileManager = FileManager("config.json")
    private val json: JsonManager = file.readJson() ?: JsonManager.Companion()
    
    var hasInitialSetup: Boolean
        get() = json.getBoolean("_init") ?: false
        set(value) = Unit.also { json["_init"] = value }
    
    var categories: Set<SupportCategory> = json.getJsonList("categories").map { SupportCategory(it) }.toSet()
        set(value) = Unit.also {
            json["categories"] = value.map { it.toJson()}
            SupportCategory.entries(value)
            field = value
        }
    
    val discord = DiscordConfig(json)
    
    fun saveConfig(async: Boolean) {
        if (json.isOriginalContent())
            return
        
        if (async) return scheduleAsync { this.saveConfig(false) }
        
        if (!file.exists)
            file.createJsonFile()
        
        file.write(json)
    }
}

data class DiscordConfig(private val json: JsonManager) {

    var logChannel: TextChannel?
        get() = json.getLong("discord.logChannel")?.let { JDA.getTextChannelById(it) }
        set(value) = Unit.also { json["discord.logChannel"] = value?.idLong }
}