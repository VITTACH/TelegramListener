package com.vittach.teleghost.ui.base.widget

import me.aartikov.sesame.localizedstring.LocalizedString

data class SnackbarAction(
    val message: LocalizedString,
    val actionTitle: LocalizedString,
    val action: () -> Unit = { }
)