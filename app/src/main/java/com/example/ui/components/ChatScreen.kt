package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Creator
import com.example.ui.viewmodel.MainViewModel
import com.example.getDrawableIdByName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val isCreatorMode by viewModel.isCreatorMode.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val creators by viewModel.creators.collectAsStateWithLifecycle()

    var showComposeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0B14).copy(alpha = 0.55f), // Translucent Midnight Purple Black
                        Color(0xFF13101C).copy(alpha = 0.55f)
                    )
                )
            )
    ) {
        if (activeConversationId == null) {
            // ==========================================
            // 1. INBOX LIST VIEW
            // ==========================================
            InboxListView(
                conversations = conversations,
                isCreatorMode = isCreatorMode,
                onSelectConversation = { viewModel.selectConversation(it) },
                onToggleHold = { id, held -> viewModel.setConversationHeldState(id, held) },
                onComposeClick = { showComposeDialog = true }
            )
        } else {
            // ==========================================
            // 2. ACTIVE CHAT DETAILS VIEW
            // ==========================================
            val activeConv = conversations.find { it.id == activeConversationId }
            ActiveChatView(
                conversation = activeConv,
                messages = activeMessages,
                isCreatorMode = isCreatorMode,
                creators = creators,
                onBackClick = { viewModel.selectConversation("") }, // Deselect
                onSendTextMessage = { text, media, premium, price ->
                    viewModel.sendMessageToActive(text, media, premium, price)
                },
                onTriggerSimulatedReply = { senderId, text, media, premium, price ->
                    viewModel.simulateReceiveMessageInActive(senderId, text, media, premium, price)
                },
                onUnlockPremium = { viewModel.unlockPremiumMessage(it) },
                onToggleHold = { id, held -> viewModel.setConversationHeldState(id, held) }
            )
        }

        // ==========================================
        // 3. COMPOSE DIALOG
        // ==========================================
        if (showComposeDialog) {
            ComposeDialog(
                creators = creators,
                onDismiss = { showComposeDialog = false },
                onSelectCreator = { creator ->
                    viewModel.createNewDirectMessage(creator.id, creator.name)
                    showComposeDialog = false
                }
            )
        }
    }
}

@Composable
fun InboxListView(
    conversations: List<Conversation>,
    isCreatorMode: Boolean,
    onSelectConversation: (String) -> Unit,
    onToggleHold: (String, Boolean) -> Unit,
    onComposeClick: () -> Unit
) {
    var selectedTierTab by remember { mutableStateOf("PRIMARY") } // "PRIMARY", "REQUESTS", "ARCHIVED"

    // Filter conversations based on selected tier and Hold state
    val filteredConversations = remember(conversations, selectedTierTab, isCreatorMode) {
        conversations.filter { conv ->
            when (selectedTierTab) {
                "PRIMARY" -> conv.tier == "PRIMARY" && (!isCreatorMode || !conv.isHeld)
                "REQUESTS" -> conv.tier == "REQUESTS" || (isCreatorMode && conv.isHeld)
                "ARCHIVED" -> conv.tier == "ARCHIVED"
                else -> true
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onComposeClick,
                containerColor = Color(0xFFD4AF37), // Metallic Gold
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 16.dp)
                    .testTag("compose_message_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Compose Message"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isCreatorMode) "Creator Inbox" else "My Messages",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Text(
                        text = if (isCreatorMode) "Manage verified subscribers & paid requests" else "Chat with your favorite models",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                Icon(
                    imageVector = if (isCreatorMode) Icons.Filled.Security else Icons.Filled.Mail,
                    contentDescription = "Inbox Status",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Tabs Bar
            TabRow(
                selectedTabIndex = when (selectedTierTab) {
                    "PRIMARY" -> 0
                    "REQUESTS" -> 1
                    "ARCHIVED" -> 2
                    else -> 0
                },
                containerColor = Color(0xFF161324),
                contentColor = Color(0xFFD4AF37),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[
                            when (selectedTierTab) {
                                "PRIMARY" -> 0
                                "REQUESTS" -> 1
                                "ARCHIVED" -> 2
                                else -> 0
                            }
                        ]),
                        color = Color(0xFFD4AF37)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTierTab == "PRIMARY",
                    onClick = { selectedTierTab = "PRIMARY" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Primary", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Primary", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    selectedContentColor = Color(0xFFD4AF37),
                    unselectedContentColor = Color.White.copy(alpha = 0.5f)
                )
                Tab(
                    selected = selectedTierTab == "REQUESTS",
                    onClick = { selectedTierTab = "REQUESTS" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = "Requests", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isCreatorMode) "Requests (Held)" else "Requests", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    selectedContentColor = Color(0xFFD4AF37),
                    unselectedContentColor = Color.White.copy(alpha = 0.5f)
                )
                Tab(
                    selected = selectedTierTab == "ARCHIVED",
                    onClick = { selectedTierTab = "ARCHIVED" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Archive, contentDescription = "Archived", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Archived", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    selectedContentColor = Color(0xFFD4AF37),
                    unselectedContentColor = Color.White.copy(alpha = 0.5f)
                )
            }

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = "Empty Inbox",
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Messages Found",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.8f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start a premium aesthetic thread or invite creators via the Compose button below.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(filteredConversations) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            isCreatorMode = isCreatorMode,
                            onClick = { onSelectConversation(conversation.id) },
                            onToggleHold = { onToggleHold(conversation.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: Conversation,
    isCreatorMode: Boolean,
    onClick: () -> Unit,
    onToggleHold: (Boolean) -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A30)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                1.dp,
                if (conversation.unreadCount > 0) Color(0xFFD4AF37).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .testTag("conversation_item_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with default fallback
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2645))
            ) {
                // Determine a nice mock avatar resource or use fallback
                val avatarName = when {
                    conversation.title.contains("Anastasia", true) -> "img_open_toes_pink_1782962118574"
                    conversation.title.contains("Sasha", true) -> "img_model2_pedicure_1781222811962"
                    conversation.title.contains("Elena", true) -> "img_model3_anklet_1781222826340"
                    else -> "img_sexy_slippers_yellow_1782962148817"
                }
                val drawableId = getDrawableIdByName(avatarName)
                if (drawableId != 0) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = conversation.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User avatar fallback",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    val formattedTime = remember(conversation.timestamp) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        sdf.format(Date(conversation.timestamp))
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.lastMessage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (conversation.lastMessage.startsWith("[")) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD4AF37), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                if (isCreatorMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (conversation.isHeld) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "Spam status",
                            tint = if (conversation.isHeld) Color(0xFFE57373) else Color(0xFF81C784),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (conversation.isHeld) "Held (Spam Protected)" else "Verified Reader",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (conversation.isHeld) Color(0xFFE57373) else Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (conversation.isHeld) "Release Hold" else "Hold Message",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            modifier = Modifier
                                .clickable { onToggleHold(!conversation.isHeld) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatView(
    conversation: Conversation?,
    messages: List<Message>,
    isCreatorMode: Boolean,
    creators: List<Creator>,
    onBackClick: () -> Unit,
    onSendTextMessage: (String, String?, Boolean, Double) -> Unit,
    onTriggerSimulatedReply: (String, String, String?, Boolean, Double) -> Unit,
    onUnlockPremium: (Message) -> Unit,
    onToggleHold: (String, Boolean) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Deduce recipient name & image from title
    val title = conversation?.title ?: "Chat"
    val conversationId = conversation?.id ?: ""
    val isHeld = conversation?.isHeld ?: false

    // Identify recipient creatorId if we are FAN talking to CREATOR
    val recipientCreatorId = remember(conversationId) {
        if (conversationId.startsWith("conv_")) {
            conversationId.substringAfter("conv_")
        } else {
            "creator_sasha"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            val avatarName = when {
                                title.contains("Anastasia", true) -> "img_open_toes_pink_1782962118574"
                                title.contains("Sasha", true) -> "img_model2_pedicure_1781222811962"
                                title.contains("Elena", true) -> "img_model3_anklet_1781222826340"
                                else -> "img_sexy_slippers_yellow_1782962148817"
                            }
                            val drawableId = getDrawableIdByName(avatarName)
                            if (drawableId != 0) {
                                Image(
                                    painter = painterResource(id = drawableId),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isHeld) "Held in Filter Stack" else "Real-time Verified Session",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isHeld) Color(0xFFE57373) else Color(0xFF81C784),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isCreatorMode) {
                        IconButton(onClick = { onToggleHold(conversationId, !isHeld) }) {
                            Icon(
                                imageVector = if (isHeld) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Spam Shield",
                                tint = if (isHeld) Color(0xFFE57373) else Color(0xFF81C784)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13101C))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Held notice banner
            if (isHeld && isCreatorMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4C1C24))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ Spam protection has held this conversation. Release hold to allow free replies.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2)),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onToggleHold(conversationId, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Release", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            // Messages LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onUnlockClick = { onUnlockPremium(message) }
                    )
                }
            }

            // Active Interactive Simulation Bar for developers to quickly reply
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161324))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🤖 Reply Simulator:",
                    color = Color(0xFFD4AF37),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onTriggerSimulatedReply(
                                recipientCreatorId,
                                "Here is an exclusive peek for you, sweetheart! 💕",
                                null,
                                false,
                                0.0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2645)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Chat Reply", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            // Send a premium PPV content photo as simulated creator reply!
                            onTriggerSimulatedReply(
                                recipientCreatorId,
                                "Special Premium Model Pose",
                                "img_sexy_slippers_yellow_1782962148817",
                                true,
                                40.0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C3C1B)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("⭐ Sim PPV", fontSize = 9.sp, color = Color(0xFFD4AF37))
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0B14))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAttachmentDialog = true }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach Content", tint = Color(0xFFD4AF37))
                }

                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Write high-fidelity message...", color = Color.White.copy(alpha = 0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1A30),
                        unfocusedContainerColor = Color(0xFF1E1A30),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("chat_input_text_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp)
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendTextMessage(textInput, null, false, 0.0)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFFD4AF37))
                }
            }
        }

        // Attachment selection dialog
        if (showAttachmentDialog) {
            AttachmentPicker(
                onDismiss = { showAttachmentDialog = false },
                onSendAttachment = { mediaRes, isPremium, price ->
                    onSendTextMessage("Media attached from portfolio", mediaRes, isPremium, price)
                    showAttachmentDialog = false
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onUnlockClick: () -> Unit
) {
    val isLocal = message.senderId == "local_user"
    val alignment = if (isLocal) Alignment.End else Alignment.Start
    val bubbleBg = if (isLocal) {
        Brush.horizontalGradient(listOf(Color(0xFF5E35B1), Color(0xFF3949AB)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF231F35), Color(0xFF1E1A30)))
    }
    val contentColor = Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isLocal) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isLocal) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    val drawableId = getDrawableIdByName(message.senderAvatar)
                    if (drawableId != 0) {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = message.senderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isLocal) 16.dp else 4.dp,
                            bottomEnd = if (isLocal) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleBg)
                    .border(
                        1.dp,
                        if (message.isPremium && !message.isUnlocked) Color(0xFFD4AF37).copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isLocal) 16.dp else 4.dp,
                            bottomEnd = if (isLocal) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                // Sender name
                if (!isLocal) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFD4AF37)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // If media is attached
                if (message.mediaUri != null) {
                    if (message.isPremium && !message.isUnlocked) {
                        // LOCKED PREMIUM CONTENT VIEW
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                // Blurred preview
                                val drawableId = getDrawableIdByName(message.mediaUri)
                                if (drawableId != 0) {
                                    Image(
                                        painter = painterResource(id = drawableId),
                                        contentDescription = "Premium item locked preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Locked Content",
                                            tint = Color(0xFFD4AF37),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "PPV Premium",
                                            fontSize = 11.sp,
                                            color = Color(0xFFD4AF37),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Unlock this high-fidelity premium post for ${message.unlockPriceCoins} Gold Coins.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onUnlockClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                modifier = Modifier.fillMaxWidth().testTag("unlock_ppv_button")
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unlock with ${message.unlockPriceCoins} Coins", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // UNLOCKED OR FREE MEDIA VIEW
                        Box(
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            val drawableId = getDrawableIdByName(message.mediaUri)
                            if (drawableId != 0) {
                                Image(
                                    painter = painterResource(id = drawableId),
                                    contentDescription = "Attached Portfolio Asset",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (message.isPremium) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "UNLOCKED PPV",
                                        color = Color(0xFF81C784),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = contentColor,
                        fontSize = 14.sp
                    )
                )
            }

            if (isLocal) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        // Bubble footer
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedTime = remember(message.timestamp) {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                sdf.format(Date(message.timestamp))
            }
            Text(
                text = formattedTime,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
            if (isLocal) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when (message.status) {
                        "SENT" -> Icons.Filled.Check
                        else -> Icons.Filled.DoneAll
                    },
                    contentDescription = message.status,
                    tint = if (message.status == "READ") Color(0xFFD4AF37) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun ComposeDialog(
    creators: List<Creator>,
    onDismiss: () -> Unit,
    onSelectCreator: (Creator) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Start Premium Thread",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Select one of our high-fidelity foot models to begin a private, secure VIP messaging session.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(creators) { creator ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCreator(creator) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                val drawableId = getDrawableIdByName(creator.avatarRes)
                                if (drawableId != 0) {
                                    Image(
                                        painter = painterResource(id = drawableId),
                                        contentDescription = creator.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    creator.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    creator.handle,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFD4AF37))
            }
        },
        containerColor = Color(0xFF1E1A30),
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPicker(
    onDismiss: () -> Unit,
    onSendAttachment: (String, Boolean, Double) -> Unit
) {
    // List of premium presets available
    val assets = listOf(
        "img_open_toes_pink_1782962118574" to "Luxury Pink Sandals",
        "img_model2_pedicure_1781222811962" to "Artistic Blue Pedicure",
        "img_model3_anklet_1781222826340" to "Elena Wilde Anklet",
        "img_sexy_slippers_yellow_1782962148817" to "Yellow Feather Slippers"
    )

    var selectedAsset by remember { mutableStateOf("img_open_toes_pink_1782962118574") }
    var isPremium by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf("30.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Attach Portfolio Asset",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Choose an aesthetic visual asset from the CreatorPortfolio to send into the chat stream.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grid of assets
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assets) { (resName, label) ->
                        val isSelected = selectedAsset == resName
                        Card(
                            onClick = { selectedAsset = resName },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF3B2F63) else Color(0xFF161324)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    val drawableId = getDrawableIdByName(resName)
                                    if (drawableId != 0) {
                                        Image(
                                            painter = painterResource(id = drawableId),
                                            contentDescription = label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Lock with PPV?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Requires viewer payment to see", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                    Switch(
                        checked = isPremium,
                        onCheckedChange = { isPremium = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD4AF37),
                            checkedTrackColor = Color(0xFF3B2F63)
                        )
                    )
                }

                if (isPremium) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Unlock Price (Gold Coins)", color = Color.White.copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 30.0
                    onSendAttachment(selectedAsset, isPremium, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
            ) {
                Text("Attach & Send", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1A30),
        shape = RoundedCornerShape(16.dp)
    )
}
