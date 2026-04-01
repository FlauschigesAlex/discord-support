package at.flauschigesalex.dsb.ktor

import at.flauschigesalex.dsb.configuration.Configuration
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.general.HttpRequestHandler
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.json.Json
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver


object KtorApplication {
    init {
        embeddedServer(
            Netty,
            configure = {
                connector {
                    port = Configuration.ktor.port
                    host = Configuration.ktor.host
                }

                /*
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = "sampleAlias",
                    keyStorePassword = { keyStorePassword },
                    privateKeyPassword = { privateKeyPassword }
                ) {
                    port = 8443
                    host = Configuration.ktor.host
                    keyStorePath = keyStoreFile
                }
                */
            }
        ) {
            install(IgnoreTrailingSlash)
            installThymeleaf()
            installDiscordOAuth()
            
            routing {
                staticResources("static/", "static/")
                
                route("/") {
                    install(RequireAuthentication)
                    
                    get {

                        val session = call.sessions.get<DiscordProfile>()!!
                        call.respondTemplate("index")
                    }
                    get("leck") {
                        val session = call.sessions.get<DiscordProfile>()!!
                        call.respondTemplate("index")
                    }
                }
                
                authenticate("discord") {
                    get("/discord/login/") {
                        // HANDLED BY DISCORD
                    }

                    get("/discord/auth/") {
                        val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                        if (principal == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Authorization failed")
                            return@get
                        }

                        val userJSON = HttpRequestHandler("https://discord.com/api/v10/users/@me")
                            ?.addHeaders("Authorization" to "Bearer ${principal.accessToken}")
                            ?.get(JsonManager.BodyHandler)
                            ?.body()
                            ?: return@get call.respond(HttpStatusCode.Unauthorized, "Authorization failed")

                        userJSON["token"] = principal.accessToken

                        val user = Json.decodeFromString<DiscordProfile>(userJSON.toString())
                        call.sessions.set(user)
                        
                        val returnUri = call.request.cookies["returnUri"] ?: "/"
                        call.respondRedirect(returnUri)
                    }
                }
            }
        }.start(wait = true)
    }
}

private fun Application.installThymeleaf() {
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
            templateMode = TemplateMode.HTML
        })
    }
}

private fun Application.installDiscordOAuth() {
    install(Sessions) {
        cookie<DiscordProfile>("session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.secure = true
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 1 // 24h / 1d
        }
    }

    install(Authentication) {
        oauth("discord") {
            urlProvider = { "http://localhost:8080/discord/auth/" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = Configuration.discord.clientId.toString(),
                    clientSecret = Configuration.discord.clientSecret,
                    defaultScopes = listOf("identify")
                )
            }
            client = HttpClient()
        }
    }
}