package at.flauschigesalex.dsb.data

import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.lib.base.file.JsonManager
import net.dv8tion.jda.api.Permission

@ExposedCopyVisibility
data class MemberData private constructor(private val json: JsonManager) : Serializable() {
    companion object {
        operator fun invoke(json: JsonManager?): MemberData = MemberData(json ?: JsonManager())
    }
    
    var allowed: Set<Permission>
        get() = json.getStringList("permissions.allowed").mapNotNull { perm -> runCatching { Permission.valueOf(perm) }.getOrNull() }.toSet()
        set(value) = Unit.also { json["permissions.allowed"] = value.map { it.name } }
    
    var denied: Set<Permission>
        get() = json.getStringList("permissions.denied").mapNotNull { perm -> runCatching { Permission.valueOf(perm) }.getOrNull() }.toSet()
        set(value) = Unit.also { json["permissions.denied"] = value.map { it.name } }
    
    override fun toJson(): JsonManager = json.clone()
}

