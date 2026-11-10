package com.celmatech.studeco.ui.screens.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celmatech.studeco.data.model.FeatureFlags
import com.celmatech.studeco.data.model.MarketplaceListing
import com.celmatech.studeco.data.model.Opportunity
import com.celmatech.studeco.data.model.ResourceItem
import com.celmatech.studeco.data.model.TaskItem
import com.celmatech.studeco.data.repository.AuthRepository
import com.celmatech.studeco.data.repository.MarketplaceRepository
import com.celmatech.studeco.data.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository,
    private val plannerRepository: PlannerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var listings by mutableStateOf<List<MarketplaceListing>>(emptyList())
        private set
    var opportunities by mutableStateOf<List<Opportunity>>(emptyList())
        private set
    var resources by mutableStateOf<List<ResourceItem>>(emptyList())
        private set
    var tasks by mutableStateOf<List<TaskItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isCreating by mutableStateOf(false)
        private set
    private var institution: String? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { institution = it.user?.institution }
        }
    }

    fun loadAll(flags: FeatureFlags) {
        viewModelScope.launch {
            isLoading = true
            error = null
            if (flags.marketplace) marketplaceRepository.getListings().onSuccess { listings = it }.onFailure { error = it.message }
            if (flags.opportunities) marketplaceRepository.getOpportunities().onSuccess { opportunities = it }
            if (flags.resources) marketplaceRepository.getResources().onSuccess { resources = it }
            if (flags.planner) plannerRepository.getTasks().onSuccess { tasks = it }
            isLoading = false
        }
    }

    fun createListing(title: String, description: String, price: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isCreating = true
            marketplaceRepository.createListing(title, description, price.toDoubleOrNull(), null, institution)
                .onSuccess { listings = listOf(it) + listings; onDone(true) }
                .onFailure { error = it.message; onDone(false) }
            isCreating = false
        }
    }

    fun createTask(title: String, notes: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isCreating = true
            plannerRepository.createTask(title, notes, null)
                .onSuccess { tasks = tasks + it; onDone(true) }
                .onFailure { error = it.message; onDone(false) }
            isCreating = false
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            plannerRepository.toggleDone(task).onSuccess { done ->
                tasks = tasks.map { if (it.id == task.id) it.copy(isDone = done) else it }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(flags: FeatureFlags, viewModel: StudyViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateListing by remember { mutableStateOf(false) }
    var showCreateTask by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }

    val tabs = buildList {
        if (flags.resources) add("Resources")
        if (flags.marketplace) add("Marketplace")
        if (flags.opportunities) add("Opportunities")
        if (flags.planner) add("Planner")
        if (flags.aiTutor) add("AI Tutor")
    }

    LaunchedEffect(flags) { viewModel.loadAll(flags) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Study Hub", fontWeight = FontWeight.SemiBold) }) },
        floatingActionButton = {
            when (tabs.getOrNull(selectedTab)) {
                "Marketplace" -> FloatingActionButton(onClick = { showCreateListing = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New listing")
                }
                "Planner" -> FloatingActionButton(onClick = { showCreateTask = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New task")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (tabs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Study features are currently disabled by the administrator.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
            ScrollableTabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
                }
            }
            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                when (tabs.getOrNull(selectedTab)) {
                    "Resources" -> SimpleList(viewModel.resources.map { it.title to (it.description ?: "") }, "No resources yet")
                    "Marketplace" -> MarketplaceList(viewModel.listings)
                    "Opportunities" -> OpportunitiesList(viewModel.opportunities)
                    "Planner" -> PlannerList(viewModel.tasks, onToggle = { viewModel.toggleTask(it) })
                    "AI Tutor" -> PlaceholderPane(Icons.Default.AutoAwesome, "AI Tutor", "Enabled by admin. Study help will appear here.")
                }
            }
        }
    }

    if (showCreateListing) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isCreating) showCreateListing = false },
            title = { Text("New marketplace listing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (MWK)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createListing(title, description, price) { ok ->
                        if (ok) { title = ""; description = ""; price = ""; showCreateListing = false }
                    }
                }, enabled = title.isNotBlank() && !viewModel.isCreating) { Text("Publish") }
            },
            dismissButton = { TextButton(onClick = { showCreateListing = false }) { Text("Cancel") } }
        )
    }

    if (showCreateTask) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isCreating) showCreateTask = false },
            title = { Text("New task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = taskNotes, onValueChange = { taskNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createTask(taskTitle, taskNotes.ifBlank { null }) { ok ->
                        if (ok) { taskTitle = ""; taskNotes = ""; showCreateTask = false }
                    }
                }, enabled = taskTitle.isNotBlank() && !viewModel.isCreating) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCreateTask = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SimpleList(items: List<Pair<String, String>>, empty: String) {
    if (items.isEmpty()) EmptyState(empty, "")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items) { (t, d) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (d.isNotBlank()) Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MarketplaceList(items: List<MarketplaceListing>) {
    if (items.isEmpty()) EmptyState("Marketplace is empty", "Tap + to list an item.")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${item.price?.toInt() ?: "—"} ${item.currency}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (!item.description.isNullOrBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text("by ${item.sellerName ?: "Student"}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun OpportunitiesList(items: List<Opportunity>) {
    if (items.isEmpty()) EmptyState("No opportunities yet", "Internships and events will show here.")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { op ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(op.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(op.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (!op.description.isNullOrBlank()) Text(op.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    if (!op.deadline.isNullOrBlank()) Text("Deadline: ${op.deadline}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PlannerList(tasks: List<TaskItem>, onToggle: (TaskItem) -> Unit) {
    if (tasks.isEmpty()) EmptyState("No tasks yet", "Tap + to add a deadline or to-do.")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { it.id }) { task ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggle(task) }) {
                        Icon(
                            if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                        )
                        if (!task.notes.isNullOrBlank()) {
                            Text(task.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaceholderPane(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
