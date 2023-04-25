package com.vittach.teleghost.ui.base

import androidx.annotation.AttrRes

data class ThemeSettings(
    val statusBarSettings: StatusBarSettings? = null
) {

    data class StatusBarSettings(
        @AttrRes val statusBarAttributeColor: Int,
        val statusBarTheme: Theme
    )

    enum class Theme {
        LIGHT, DARK
    }
}