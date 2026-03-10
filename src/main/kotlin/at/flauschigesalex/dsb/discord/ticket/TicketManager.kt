package at.flauschigesalex.dsb.discord.ticket

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.dsb.configuration.GuildConfiguration
import at.flauschigesalex.dsb.configuration.config
import at.flauschigesalex.dsb.scheduleAsync
import at.flauschigesalex.lib.base.file.FileManager
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.file.readJson
import net.dv8tion.jda.api.entities.Guild

class TicketManager(private val parent: GuildConfiguration, private val file: FileManager) {
    companion object {
        fun all(): List<TicketManager> = Configuration.guildConfigs.map { it.ticketManager }
        init {
            Runtime.getRuntime().addShutdownHook(Thread { all().forEach { it.saveAll(false) } })
        }
    }
    
    val json: JsonManager = file.readJson() ?: JsonManager()
    val tickets: Set<SupportTicket> get() = _tickets.toSet()
    
    
    private var _tickets: Set<SupportTicket> =
        json.getJsonList("tickets").mapNotNull { SupportTicket(parent, it) }.toSet()
        set(value) {
            json["tickets"] = value.map { it.toJson() }
            field = value
        }
    
    fun updateTicket(ticket: SupportTicket) {
        _tickets -= ticket
        _tickets += ticket
    }
    fun removeTicket(ticket: SupportTicket) {
        _tickets -= ticket
    }
    
    fun saveAll(async: Boolean) {
        if (json.isOriginalContent()) return
        
        if (async) return scheduleAsync { saveAll(false) }

        if (!file.exists) file.createFile()
        file.write(json)
    }
}

val Guild.ticketManager: TicketManager?
    get() = config?.ticketManager

val Guild.ticketsOrNull: Set<SupportTicket>?
    get() = ticketManager?.tickets
val Guild.tickets: Set<SupportTicket>
    get() = ticketsOrNull ?: emptySet()