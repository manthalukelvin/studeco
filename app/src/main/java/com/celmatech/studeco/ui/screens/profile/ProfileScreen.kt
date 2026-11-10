package com.celmatech.studeco.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celmatech.studeco.data.model.Profile
import com.celmatech.studeco.data.repository.AuthRepository
import com.celmatech.studeco.data.repository.FriendsRepository
import com.celmatech.studeco.data.repository.Friendship
import com.celmatech.studeco.ui.screens.notifications.NotificationsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val authRepository: AuthRepository,
    private val friendsRepository: FriendsRepository,
    private val client: SupabaseClient
) : ViewModel() {
    var user by mutableStateOf<Profile?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var friends by mutableStateOf<List<Friendship>>(emptyList())
        private set
    var pending by mutableStateOf<List<Friendship>>(emptyList())
        private set
    var showFriends by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { user = it.user }
        }
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            val id = user?.id ?: return@launch
            isSaving = true
            try {
                client.from("profiles").update(mapOf("bio" to bio)) {
                    filter { eq("id", id) }
                }
                authRepository.refreshProfile()
                message = "Profile updated"
            } catch (e: Exception) {
                message = e.message ?: "Update failed"
            }
            isSaving = false
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.getFriends().onSuccess { friends = it }
            friendsRepository.getPendingRequests().onSuccess { pending = it }
            showFriends = true
        }
    }

    fun closeFriends() { showFriends = false }

    fun respondRequest(id: String, accept: Boolean) {
        viewModelScope.launch {
            friendsRepository.respond(id, accept)
            loadFriends()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user = viewModel.user
    var showEditBio by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var bioText by remember { mutableStateOf("") }

    if (showNotifications) {
        NotificationsScreen(onBack = { showNotifications = false })
        return
    }

    if (viewModel.showFriends) {
        FriendsSheet(viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showNotifications = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.fullName?.take(1)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user?.fullName ?: "Student", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (user?.isVerified == true) {
                    Icon(Icons.Default.Verified, "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (!user?.institution.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.School, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(user?.institution ?: "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!user?.programme.isNullOrBlank()) {
                Text(user?.programme ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Plan: ${user?.plan?.replaceFirstChar { it.uppercase() } ?: "Free"}  ·  ${user?.role ?: "student"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (user?.isSuspended == true) {
                Spacer(Modifier.height(8.dp))
                Text("Account suspended", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))

            // Quick actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showNotifications = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Alerts")
                }
                OutlinedButton(onClick = { viewModel.loadFriends() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.People, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Friends")
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = {
                            bioText = user?.bio ?: ""
                            showEditBio = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit bio")
                        }
                    }
                    Text(
                        text = user?.bio?.ifBlank { "No bio yet. Tell your campus about yourself." } ?: "No bio yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (viewModel.message != null) {
                Spacer(Modifier.height(12.dp))
                Text(viewModel.message!!, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  Log out")
            }
        }
    }

    if (showEditBio) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isSaving) showEditBio = false },
            title = { Text("Edit bio") },
            text = {
                OutlinedTextField(
                    value = bioText,
                    onValueChange = { bioText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateBio(bioText)
                    showEditBio = false
                }, enabled = !viewModel.isSaving) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditBio = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsSheet(viewModel: ProfileViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeFriends() }) {
                        Icon(Icons.Default.People, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding)
        ) {
            if (viewModel.pending.isNotEmpty()) {
                item {
                    Text("Pending requests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                items(viewModel.pending, key = { it.id }) { f ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f.otherUser?.fullName ?: "Student", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            TextButton(onClick = { viewModel.respondRequest(f.id, true) }) { Text("Accept") }
                            TextButton(onClick = { viewModel.respondRequest(f.id, false) }) { Text("Decline") }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
            item {
                Text("Friends", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (viewModel.friends.isEmpty()) {
                item {
                    Text("No friends yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(viewModel.friends, key = { it.id }) { f ->
                    ListItem(
                        headlineContent = { Text(f.otherUser?.fullName ?: "Student") },
                        supportingContent = { Text(f.otherUser?.institution ?: "") }
                    )
                }
            }
        }
    }
}
