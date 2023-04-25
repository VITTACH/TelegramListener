package com.vittach.teleghost.services.utils

import com.vittach.teleghost.domain.ChatMessage
import org.drinkless.td.libcore.telegram.TdApi.*

/**
 * Created by VITTACH on 26.04.2022.
 */

private fun MessageSender.fetchSenderId() = when (this) {
    is MessageSenderUser -> this.userId
    is MessageSenderChat -> this.chatId
    else -> null
}

fun Message.toChatMessage() = when (content) {
    is MessageText -> ChatMessage(
        message = (content as MessageText).text.text,
        senderId = senderId.fetchSenderId()
    )

    is MessageAnimatedEmoji -> ChatMessage(
        message = (content as MessageAnimatedEmoji).emoji,
        senderId = senderId.fetchSenderId()
    )

    is MessageSticker -> ChatMessage(
        sticker = (content as? MessageSticker)?.sticker?.sticker
    )

    else -> null
}

fun String.fetchRedEnvelope(): Pair<Boolean, String?> {
    val passwordReg = "【\\w*[^0-9a-zA-Z]*\\w*】".toRegex()
    val symbolsReg = "[^0-9a-zA-Z?]".toRegex()
    val END_PASSWORD = "】"
    val START_PASSWORD = "【"

    val passwords = passwordReg.find(this)?.groupValues ?: emptyList()

    var message: StringBuilder? = null
    for (password in passwords) {
        val result = password.replace(symbolsReg, "")
        if (result.length != 7) {
            continue
        }

        if (message == null) {
            message = StringBuilder()
        }
        message.append("\n" + START_PASSWORD + result + END_PASSWORD)
    }

    return (message != null) to message?.toString()
}