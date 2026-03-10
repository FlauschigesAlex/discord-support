@file:Suppress("unused")

package at.flauschigesalex.dsb.data

import at.flauschigesalex.dsb.utils.Serializable
import at.flauschigesalex.lib.base.file.JsonManager
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message

data class MessageObject(private val json: JsonManager) : Serializable() {
    companion object {
        operator fun invoke(message: Message): MessageObject {
            val member = message.member!!
            val author = AuthorObject(member)
            
            val json = JsonManager(
                "_id" to message.idLong,
                "author" to author.toJson(),
                "attachments" to message.attachments.map { it.url },
                "content" to message.contentDisplay,
                "created" to message.timeCreated.toEpochSecond(),
                "edited" to message.timeEdited?.toEpochSecond(),
            )

            return MessageObject(json)
        }
    }
    
    val id: Long = json.getLong("_id")!!
    val author: AuthorObject = AuthorObject(json.getJson("author")!!)
    
    val attachments: List<String> = json.getStringList("attachments")
    val content: String = json.getString("content") ?: ""
    
    val edited = json.getLong("edited")
    val created = json.getLong("created")!!
    val lastInteraction = edited ?: created
    
    val isEdited = json["edited"] != null

    override fun toJson(): JsonManager = this.json.clone()
    override fun hashCode(): Int = this.id.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is MessageObject) return false
        return this.id == other.id
    }
}

data class AuthorObject(private val json: JsonManager) : Serializable() {
    companion object {
        operator fun invoke(author: Member): AuthorObject {
            val json = JsonManager(
                "_id" to author.idLong,
                "name" to author.effectiveName,
                "nickname" to author.nickname,
                "avatar" to author.user.avatarUrl
            )
            
            return AuthorObject(json)
        }
    }
    
    val id = json.getLong("_id")!!
    val name = json.getString("name")!!
    val nickname = json.getString("nickname") ?: name
    val avatar = json.getString("avatar")!!

    override fun toJson(): JsonManager = this.json.clone()
    override fun hashCode(): Int = this.id.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is AuthorObject) return false
        return this.id == other.id
    }
}