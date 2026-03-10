@file:OptIn(ExperimentalSerializationApi::class)

package at.flauschigesalex.dsb.ktor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class DiscordProfile(val token: String,
                          val id: String,
                          val username: String,
                          @SerialName("avatar") private val avatarId: String?,
                          @SerialName("banner")  val bannerId: String?,
                          @SerialName("global_name") val name: String,
) {
    companion object
    
    val idLong: Long = id.toLong()
    
    val avatarUrl: String? = avatarId?.let { "https://cdn.discordapp.com/avatars/$id/$it" }
    val bannerUrl: String? = avatarId?.let { "https://cdn.discordapp.com/banners/$id/$it" }
}
