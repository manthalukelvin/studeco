package com.celmatech.studeco.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celmatech.studeco.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {
    val isDarkTheme = prefs.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDarkTheme(enabled)
        }
    }
}
