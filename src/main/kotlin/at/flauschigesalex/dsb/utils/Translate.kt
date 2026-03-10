@file:Suppress("unused")

package at.flauschigesalex.dsb.utils

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.Locale
import java.util.ResourceBundle

val MessageChannel.locale: Locale
    get() = (this as? GuildMessageChannel)?.guild?.locale?.toLocale() ?: Locale.getDefault()

fun MessageChannel.sendLocalizedMessage(key: String, locale: Locale, vararg data: Any?): MessageCreateAction {
    return this.sendMessage(locale.translate(key, *data))
}
fun MessageChannel.sendLocalizedMessage(key: String, vararg data: Any?): MessageCreateAction {
    return this.sendLocalizedMessage(key, this.locale, *data)
}
fun IReplyCallback.replyLocalized(key: String, ephemeral: Boolean, vararg data: Any?): ReplyCallbackAction {
    val locale = this.guild?.locale?.toLocale() ?: Locale.getDefault()
    return this.reply(locale.translate(key, *data)).setEphemeral(ephemeral)
}
fun IReplyCallback.replyLocalized(key: String, vararg data: Any?) = this.replyLocalized(key, false, *data)

fun Locale.translate(key: String, vararg data: Any?) = ResourceBundle.getBundle("i18n.messages", this).getString(key).format(*data)