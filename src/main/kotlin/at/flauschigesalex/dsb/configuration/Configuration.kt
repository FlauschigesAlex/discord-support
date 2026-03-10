package at.flauschigesalex.dsb.configuration

import at.flauschigesalex.dsb.JDA
import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.discord.ticket.TicketManager
import at.flauschigesalex.dsb.logger
import at.flauschigesalex.dsb.scheduleAsync
import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.lib.base.file.Environment
import at.flauschigesalex.lib.base.file.FileManager
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.file.readJson
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion
import java.io.File

object Configuration {
    
    private val DIR_SOURCE: String = Environment["OVERRIDE_CONFIG_DIR"] ?: "configs"
    private val TYPE_SOURCE: String = Environment["OVERRIDE_TYPE_DIR"] ?: "types"
    
    private val _guildConfigs = mutableSetOf<GuildConfiguration>()
    val guildConfigs: Set<GuildConfiguration>
        get() = _guildConfigs.toSet()
    
    val discord: DiscordConfiguration = DiscordConfiguration(FileManager(File(DIR_SOURCE, "discord"), "config.json"))
    
    init {
        // CHECK FOR DIRECTORY
        val dir = FileManager("${DIR_SOURCE}/${TYPE_SOURCE}")
        if (!dir.exists) dir.createDirectory()
        require(dir.file.isDirectory) { "$TYPE_SOURCE must be a directory" }
        
        // CREATE GUILD CONFIGS FROM FILES
        dir.listFiles.mapNotNull { parent ->
            if (!parent.file.isDirectory) {
                logger.warn("Config parent directory must only contain directories, found: $parent")
                return@mapNotNull null
            }
            
            val config = FileManager(parent, "config.json")
            if (!config.exists) {
                logger.warn("Config directory must contain a 'config.json': $config")
                return@mapNotNull null
            }
            
            val gc = GuildConfiguration(config, FileManager(parent, "tickets.json")) ?: return@mapNotNull null
            _guildConfigs += gc
            return@mapNotNull gc
        }
    }
}

@ConsistentCopyVisibility
data class DiscordConfiguration internal constructor(private val file: FileManager): Serializable() {
    private val json = file.readJson() ?: JsonManager()
    
    var token: String? = json.getString("bot.token")
        set(value) {
            field = value
            json["bot.token"] = value
        }
    
    var clientId: Long = json.getLong("client.id") ?: throw IllegalStateException("Missing 'client.id' in config.json")
        set(value) {
            field = value
            json["client.id"] = value
        }
    
    var clientSecret: String = json.getString("client.secret") ?: throw IllegalStateException("Missing 'client.secret' in config.json")
        set(value) {
            field = value
            json["client.secret"] = value
        }

    fun save(async: Boolean) {
        if (json.isOriginalContent()) return
        if (async) return scheduleAsync { this.save(false) }
        
        if (!file.exists) file.createFile()
        file.write(json)
    }
    
    override fun toJson(): JsonManager = json.clone()
}

@ConsistentCopyVisibility
data class GuildConfiguration private constructor(val idLong: Long,
                                                  private val config: FileManager,
                                                  private val ticketFile: FileManager,
                                                  private val json: JsonManager
) : Serializable() {
    companion object {
        private const val VERSION = 1
        
        operator fun invoke(config: FileManager, tickets: FileManager): GuildConfiguration? {
            val json = config.readJson() ?: run {
                logger.warn("$config does not contain valid JSON: ${config.readString()}")
                return null
            }
            
            val idLong = json.getLong("_id") ?: run {
                logger.warn("$config does not contain field '_id': $json")
                return null
            }
            
            return GuildConfiguration(idLong, config, tickets, json)
        }
    }

    val id: String = idLong.toString()
    var version = json.getInt("_version") ?: 0 // TODO
        private set(value) {
            json["_version"] = value
            field = value
        }
    
    var supportCategories: Set<SupportCategory> =
        json.getJsonList("support.categories").mapNotNull { SupportCategory(this, it) }.toSet()
        set(value) {
            json["support.categories"] = value.map { it.toJson() }
            field = value
        }
    val ticketManager: TicketManager = TicketManager(this, ticketFile)

    val guild: Guild?
        get() = JDA.getGuildById(idLong) ?: run {
            logger.warn("$config: Guild with id $id could not be found.")
            return@run null
        }

    var channel: GuildMessageChannelUnion? =
        json.getLong("channels.admin")?.let { guild?.getGuildChannelById(it) as? GuildMessageChannelUnion }
        set(value) {
            json["channels.admin"] = value?.idLong
            field = value
        }

    fun save(async: Boolean) {
        ticketManager.saveAll(async)
        if (json.isOriginalContent()) return

        if (async) return scheduleAsync { this.save(false) }

        this.saveConfigFile(false)
    }
    private fun saveConfigFile(isFallback: Boolean) {
        val file = if (isFallback.not()) config else FileManager("$config.fallback")
        if (json.isOriginalContent()) return

        val created = file.exists || file.createFile() != null
        val written = file.write(json)

        if (isFallback) {
            if (created && written) {
                logger.warn("Failed to write config file, changes are backed up at $file.")
                return
            }

            if (!written) return logger.error("Failed to write config file $file.")
            return logger.error("Failed to create config file $file.")
        }

        if (created && written) return
        this.saveConfigFile(true)
    }

    override fun toJson(): JsonManager = json.clone()
    override fun hashCode(): Int = idLong.hashCode()
    override fun equals(other: Any?): Boolean = other is GuildConfiguration && idLong == other.idLong
}

val Guild.config: GuildConfiguration?
    get() = Configuration.guildConfigs.find { it.idLong == idLong }