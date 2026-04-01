package at.flauschigesalex.dsb.ktor

import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.general.HttpRequestHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.json.Json

val RequireAuthentication = createRouteScopedPlugin("RequireAuthentication") {
    onCall { call ->
        suspend fun PipelineCall.sendAuthenticate() {
            this.response.cookies.append(Cookie("returnUri", this.request.uri, maxAge = 2 * 60))
            this.respondTemplate("authenticate")
        }
        
        val session = call.sessions.get<DiscordProfile>() ?: return@onCall call.sendAuthenticate()
        val token = session.token

        val userJSON = HttpRequestHandler("https://discord.com/api/v10/users/@me")
            ?.addHeaders("Authorization" to "Bearer $token")
            ?.get(JsonManager.BodyHandler)
            ?.body()
            ?: return@onCall call.sendAuthenticate()

        userJSON["token"] = token
        
        val user = Json.decodeFromString<DiscordProfile>(userJSON.toString())
        call.sessions.set(user)
    }
}