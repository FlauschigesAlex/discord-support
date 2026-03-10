package at.flauschigesalex.dsb.data

import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.lib.base.file.JsonManager

@ExposedCopyVisibility
data class RoleData private constructor(val id: Long, private val json: JsonManager) : Serializable() {

    companion object {
        operator fun invoke(json: JsonManager): RoleData? {
            val id = json.getLong("_id") ?: return null
            return RoleData(id, json)
        }
    }

    var flags: Set<RoleFlag>
        get() = json.getStringList("flags").mapNotNull { flag -> RoleFlag.entries.find { it.name.equals(flag, true) } }.toSet()
        set(value) = Unit.also { json["flags"] = value.map { it.name } }
    fun isFlag(flag: RoleFlag) = flags.contains(flag)

    val permissions = MemberData(json)

    override fun toJson(): JsonManager = json.clone()
}

enum class RoleFlag {
    CLOSE_TICKET,
    IS_MENTION,
    
    ;
}