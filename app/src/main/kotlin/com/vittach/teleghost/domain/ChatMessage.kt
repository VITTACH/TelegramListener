package com.vittach.teleghost.domain

import org.drinkless.td.libcore.telegram.TdApi

/**
 * Created by VITTACH on 08.09.2022.
 */
data class ChatMessage(
    val message: String? = null,
    val senderId: Long? = null,
    val sticker: TdApi.File? = null
)