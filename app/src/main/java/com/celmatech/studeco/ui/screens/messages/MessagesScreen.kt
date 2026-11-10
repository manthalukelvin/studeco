package com.celmatech.studeco.ui.screens.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
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
import com.celmatech.studeco.data.model.ChatMessage
import com.celmatech.studeco.data.model.Conversation
import com.celmatech.studeco.data.repository.AuthRepository
import com.celmatech.studeco.data.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var activeConversationId by mutableStateOf<String?>(null)
        private set
    var activeTitle by mutableStateOf("")
        private set
    var myUserId by mutableStateOf<String?>(null)
        private set

    init {
        myUserId = authRepository.currentUserId()
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            isLoading = true
            error = null
            messagingRepository.getMyConversations()
                .onSuccess { conversations = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun openChat(conv: Conversation) {
        activeConversationId = conv.id
        activeTitle = conv.otherUserName ?: conv.title ?: "Chat"
        viewModelScope.launch {
            messagingRepository.getMessages(conv.id)
                .onSuccess { messages = it }
                .onFailure { error = it.message }
        }
    }

    fun closeChat() {
        activeConversationId = null
        messages = emptyList()
        loadConversations()
    }

    fun send(content: String) {
        val convId = activeConversationId ?: return
        viewModelScope.launch {
            isSending = true
            messagingRepository.sendMessage(convId, content)
                .onSuccess { msg -> messages = messages + msg }
                .onFailure { error = it.message }
            isSending = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(viewModel: MessagesViewModel = hiltViewModel()) {
    if (viewModel.activeConversationId != null) {
        ChatScreen(viewModel)
    } else {
        ConversationListScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListScreen(viewModel: MessagesViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.loadConversations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.error != null && viewModel.conversations.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadConversations() }) { Text("Retry") }
                }
            }
            viewModel.conversations.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Chat, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.6f))
                    Spacer(Modifier.height(12.dp))
                    Text("No conversations yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Chats you start with classmates will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(viewModel.conversations, key = { it.id }) { conv ->
                    ListItem(
                        headlineContent = {
                            Text(conv.otherUserName ?: "Chat", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(conv.lastMessage ?: "Tap to open", maxLines = 1)
                        },
                        leadingContent = {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            (conv.otherUserName ?: "?").take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openChat(conv) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(viewModel: MessagesViewModel) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.activeTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeChat() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Message…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !viewModel.isSending) {
                            viewModel.send(input)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !viewModel.isSending
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(viewModel.messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == viewModel.myUserId
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (!isMine) {
                                Text(
                                    msg.senderName ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                msg.content,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
