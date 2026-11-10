package com.celmatech.studeco.ui.screens.communities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celmatech.studeco.data.model.Group
import com.celmatech.studeco.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {
    var groups by mutableStateOf<List<Group>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isWorking by mutableStateOf(false)
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            groupRepository.getGroups()
                .onSuccess { groups = it }
                .onFailure { error = it.message ?: "Failed to load groups" }
            isLoading = false
        }
    }

    fun joinOrLeave(group: Group) {
        viewModelScope.launch {
            isWorking = true
            val result = if (group.isMember) groupRepository.leaveGroup(group.id)
            else groupRepository.joinGroup(group.id)
            result.onSuccess {
                groups = groups.map {
                    if (it.id == group.id) it.copy(isMember = !group.isMember) else it
                }
            }.onFailure { error = it.message }
            isWorking = false
        }
    }

    fun createGroup(name: String, description: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isWorking = true
            groupRepository.createGroup(name, description)
                .onSuccess { g ->
                    groups = listOf(g) + groups
                    onDone(true)
                }
                .onFailure {
                    error = it.message
                    onDone(false)
                }
            isWorking = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(viewModel: CommunitiesViewModel = hiltViewModel()) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create group")
            }
        }
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.error != null && viewModel.groups.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
            viewModel.groups.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No groups yet. Create the first study group!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(viewModel.groups, key = { it.id }) { group ->
                    GroupCard(group, enabled = !viewModel.isWorking) {
                        viewModel.joinOrLeave(group)
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isWorking) showCreate = false },
            title = { Text("Create study group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createGroup(name, description) { ok ->
                            if (ok) {
                                name = ""; description = ""; showCreate = false
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !viewModel.isWorking
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }, enabled = !viewModel.isWorking) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupCard(group: Group, enabled: Boolean, onJoinLeave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!group.description.isNullOrBlank()) {
                    Text(
                        group.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (group.isPrivate) {
                    Text("Private", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Button(
                onClick = onJoinLeave,
                enabled = enabled,
                colors = if (group.isMember)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                else ButtonDefaults.buttonColors()
            ) {
                Text(if (group.isMember) "Leave" else "Join")
            }
        }
    }
}
