package com.celmatech.studeco.ui.screens.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
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
import com.celmatech.studeco.data.model.Comment
import com.celmatech.studeco.data.model.Post
import com.celmatech.studeco.data.repository.AuthRepository
import com.celmatech.studeco.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isPosting by mutableStateOf(false)
        private set
    var comments by mutableStateOf<List<Comment>>(emptyList())
        private set
    var commentsLoading by mutableStateOf(false)
        private set
    var activePostId by mutableStateOf<String?>(null)
        private set

    private var currentInstitution: String? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { state ->
                currentInstitution = state.user?.institution
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            feedRepository.getFeed()
                .onSuccess { posts = it }
                .onFailure { error = it.message ?: "Failed to load feed" }
            isLoading = false
        }
    }

    fun createPost(content: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isPosting = true
            feedRepository.createPost(content, currentInstitution, "institution")
                .onSuccess { newPost ->
                    posts = listOf(newPost) + posts
                    onDone(true)
                }
                .onFailure {
                    error = it.message
                    onDone(false)
                }
            isPosting = false
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            feedRepository.toggleLike(post.id).onSuccess { liked ->
                posts = posts.map {
                    if (it.id == post.id) it.copy(
                        likedByMe = liked,
                        likesCount = (it.likesCount + if (liked) 1 else -1).coerceAtLeast(0)
                    ) else it
                }
            }
        }
    }

    fun openComments(postId: String) {
        activePostId = postId
        viewModelScope.launch {
            commentsLoading = true
            feedRepository.getComments(postId)
                .onSuccess { comments = it }
                .onFailure { comments = emptyList() }
            commentsLoading = false
        }
    }

    fun closeComments() {
        activePostId = null
        comments = emptyList()
    }

    fun addComment(content: String) {
        val postId = activePostId ?: return
        viewModelScope.launch {
            feedRepository.addComment(postId, content).onSuccess { c ->
                comments = comments + c
                posts = posts.map {
                    if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    var showCreate by remember { mutableStateOf(false) }
    var newContent by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Feed", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New post")
            }
        }
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.error != null && viewModel.posts.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
            viewModel.posts.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No posts yet", style = MaterialTheme.typography.titleMedium)
                    Text("Be the first to share with your campus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(viewModel.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLike = { viewModel.toggleLike(post) },
                        onComment = { viewModel.openComments(post.id) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isPosting) showCreate = false },
            title = { Text("New post") },
            text = {
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    placeholder = { Text("What's happening on campus?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 6
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPost(newContent) { ok ->
                            if (ok) { newContent = ""; showCreate = false }
                        }
                    },
                    enabled = newContent.isNotBlank() && !viewModel.isPosting
                ) {
                    if (viewModel.isPosting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }, enabled = !viewModel.isPosting) { Text("Cancel") }
            }
        )
    }

    if (viewModel.activePostId != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.closeComments() }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                if (viewModel.commentsLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (viewModel.comments.isEmpty()) {
                    Text("No comments yet. Be the first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(viewModel.comments, key = { it.id }) { c ->
                            Column {
                                Text(c.authorName ?: "Student", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                Text(c.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Add a comment…") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                viewModel.addComment(commentInput)
                                commentInput = ""
                            }
                        },
                        enabled = commentInput.isNotBlank()
                    ) { Text("Send") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, onLike: () -> Unit, onComment: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(post.authorName ?: "Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!post.institution.isNullOrBlank()) {
                Text(post.institution, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onComment, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Forum, contentDescription = "Comments", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
