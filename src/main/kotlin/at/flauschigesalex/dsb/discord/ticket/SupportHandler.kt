@file:OptIn(TicketInternal::class)

package at.flauschigesalex.dsb.discord.ticket

import at.flauschigesalex.dsb.data.SupportCategory
import at.flauschigesalex.dsb.scheduleAsync
import at.flauschigesalex.lib.base.file.FileManager
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.file.readJson

object SupportHandler {
    
    private val file: FileManager = FileManager("tickets.json").apply { 
        if (!this.exists) this.createJsonFile()
    }
    private val json: JsonManager = file.readJson() ?: JsonManager()
    
    init {
        this.json.keys.forEach { key ->
            val category = SupportCategory.entries.find { it.id == key } ?: return@forEach
            
            val list = json.getJsonList(key)
            if (list.isEmpty()) return@forEach

            list.forEach { ticketJson ->
                runCatching {
                    SupportTicket(category, ticketJson)
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread { this.save(false) })
    }
    
    fun save(async: Boolean) {
        SupportCategory.entries.forEach { category ->
            val tickets = SupportTicket.entries
                .filter { it.category == category }
                .map { it.toJson() }
            
            json[category.id] = tickets
        }
        
        if (json.isOriginalContent()) return

        if (async) scheduleAsync {
            return@scheduleAsync this.save(false)
        }
        
        if (!file.exists) file.createJsonFile()
        file.write(json)
    }
}