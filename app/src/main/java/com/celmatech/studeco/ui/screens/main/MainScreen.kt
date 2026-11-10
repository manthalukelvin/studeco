package com.celmatech.studeco.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.celmatech.studeco.data.repository.FeatureFlagRepository
import com.celmatech.studeco.ui.screens.communities.CommunitiesScreen
import com.celmatech.studeco.ui.screens.feed.FeedScreen
import com.celmatech.studeco.ui.screens.messages.MessagesScreen
import com.celmatech.studeco.ui.screens.profile.ProfileScreen
import com.celmatech.studeco.ui.screens.study.StudyScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val featureFlags: FeatureFlagRepository
) : ViewModel()

@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val flags by viewModel.featureFlags.flags.collectAsState()

    val tabs = buildList {
        add(TabItem("Home", Icons.Default.Home) { FeedScreen() })
        if (flags.groups) add(TabItem("Communities", Icons.Default.Groups) { CommunitiesScreen() })
        if (flags.messaging) add(TabItem("Messages", Icons.Default.Message) { MessagesScreen() })
        if (flags.resources || flags.marketplace) add(TabItem("Study", Icons.Default.MenuBook) { StudyScreen(flags) })
        add(TabItem("Profile", Icons.Default.Person) { ProfileScreen(onSignOut = onSignOut) })
    }

    // Guard against index out of bounds when flags change
    if (selectedTab >= tabs.size) selectedTab = 0

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            tabs.getOrNull(selectedTab)?.content?.invoke()
        }
    }
}

private data class TabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val content: @Composable () -> Unit
)
