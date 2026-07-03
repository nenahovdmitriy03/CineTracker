package com.nenah.cinetracker.data

import android.content.Context
import com.nenah.cinetracker.model.CineAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "cine_tracker_settings",
        Context.MODE_PRIVATE
    )
    private val _theme = MutableStateFlow(currentTheme)

    val theme: StateFlow<CineAppTheme> = _theme.asStateFlow()

    val currentTheme: CineAppTheme
        get() = CineAppTheme.fromRoute(preferences.getString(KEY_THEME, CineAppTheme.Cinema.routeValue))

    fun setTheme(theme: CineAppTheme) {
        preferences.edit().putString(KEY_THEME, theme.routeValue).apply()
        _theme.value = theme
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
