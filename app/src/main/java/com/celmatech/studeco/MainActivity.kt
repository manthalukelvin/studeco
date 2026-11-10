package com.celmatech.studeco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.celmatech.studeco.ui.navigation.StudeCoNavGraph
import com.celmatech.studeco.ui.theme.StudeCoTheme
import com.celmatech.studeco.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeVm: ThemeViewModel = hiltViewModel()
            val darkTheme by themeVm.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())

            StudeCoTheme(darkTheme = darkTheme) {
                StudeCoNavGraph()
            }
        }
    }
}
