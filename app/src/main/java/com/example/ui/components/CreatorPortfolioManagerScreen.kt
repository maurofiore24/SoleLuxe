package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.service.AiEditorialEngine
import com.example.service.PricingEngine
import com.example.service.RoyaltyCalculator
import com.example.service.DeadManSwitchService
import com.example.service.AntiBotShield
import com.example.service.ShieldVerdict
import com.example.data.model.CreatorAlbum
import com.example.data.model.CreatorMediaItem
import com.example.getDrawableIdByName
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorPortfolioManagerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val account by viewModel.userAccount.collectAsStateWithLifecycle()
    val albums by viewModel.creatorAlbums.collectAsStateWithLifecycle()
    val mediaItems by viewModel.creatorMedia.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("portfolio") } // "portfolio", "albums", "creative_suite", "growth_intelligence", "settings"

    // Creative Suite Tab State
    var csSelectedVisual by remember { mutableStateOf("img_open_toes_pink_1782962118574") }
    var csSelectedStyle by remember { mutableStateOf(AiEditorialEngine.MagazineStyle.VOGUE_CLASSIC) }
    var csArOverlayType by remember { mutableStateOf("NONE") } // "NONE", "ANKLET", "STRAPS", "VOGUE_FRAME"
    var csTextOverlay by remember { mutableStateOf("SOLELUXE COUTURE") }
    var csTrimStart by remember { mutableFloatStateOf(0f) }
    var csTrimEnd by remember { mutableFloatStateOf(10f) }
    var csIsProcessing by remember { mutableStateOf(false) }
    var csProgress by remember { mutableFloatStateOf(0f) }
    val csLogs = remember { mutableStateListOf<String>() }
    var csIsPreOrder by remember { mutableStateOf(false) }

    // Growth Intel Tab State
    var giCollaboratorId by remember { mutableStateOf<String?>("Sasha") } // Sasha, Elena, None
    var giCollabSplit by remember { mutableFloatStateOf(30f) }
    var giProspectivePrice by remember { mutableStateOf("100.0") }
    var giEnableDynamicPricing by remember { mutableStateOf(false) }
    var giEnableDeadManSwitch by remember { mutableStateOf(false) }
    var giDeadManDays by remember { mutableFloatStateOf(14f) }

    // Dialog flags
    var showAddAlbumDialog by remember { mutableStateOf(false) }
    var showUploadMediaDialog by remember { mutableStateOf(false) }
    var selectedMediaPreview by remember { mutableStateOf<CreatorMediaItem?>(null) }
    var selectedAlbumDetail by remember { mutableStateOf<CreatorAlbum?>(null) }

    // Preset premium aesthetic media choices for easy uploader selection
    val aestheticPresets = listOf(
        "img_open_toes_pink_1782962118574" to "Anastasia Rose Pink Sandals",
        "img_model2_pedicure_1781222811962" to "Sasha Blue Satin Pedicure",
        "img_model3_anklet_1781222826340" to "Elena Wilde Summer Anklet",
        "img_sexy_slippers_yellow_1782962148817" to "Clara Yellow Feather Slippers",
        "img_beach_shower_1783035001001" to "Emerald Coast Shower (Turquoise)",
        "img_speedboat_duo_1783035002002" to "Yacht Club Sunsets (Orange/Fuchsia)",
        "img_dancer_motion_1783035003003" to "Ballroom Rhythm Motion (Violet)",
        "img_purple_bg_1782877584581" to "Luxury Cosmic Purple Glow",
        "img_app_icon_1781222852965" to "SoleLuxe Emblem Logo"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. HERO COVER BANNER AND PROFILE SNAP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            val bannerId = getDrawableIdByName(account?.bannerRes ?: "img_beach_shower_1783035001001")
            Image(
                painter = painterResource(id = bannerId),
                contentDescription = "Cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .blur(if (account?.bannerRes.isNullOrEmpty()) 20.dp else 0.dp)
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF090A0F).copy(alpha = 0.85f), Color(0xFF090A0F))
                        )
                    )
            )

            // Overlapping Circle Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 0.dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))))
                    .padding(3.dp)
            ) {
                val avatarId = getDrawableIdByName(account?.avatarRes ?: "img_app_icon_1781222852965")
                Image(
                    painter = painterResource(id = avatarId),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Quick Badge indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 82.dp, bottom = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(2.dp, Color(0xFF090A0F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Online",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // 2. CREATOR DETAILS & STATS SUMMARY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account?.username ?: "Unnamed Creator",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = account?.handle ?: "@handle",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CREATOR PORTFOLIO",
                        color = Color(0xFFEC4899),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Text(
                text = if (account?.bio.isNullOrBlank()) "No bio written yet. Click Portfolio Settings to write a lovely bio and attract premium subscribers!" else account?.bio!!,
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Pricing chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Paid, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Monthly Subscription", fontSize = 10.sp, color = Color.Gray)
                            Text("${account?.subPrice ?: 9.99} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.FolderCopy, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Portfolio Stats", fontSize = 10.sp, color = Color.Gray)
                            Text("${albums.size} Folders • ${mediaItems.size} Items", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Tab Navigation for Portfolio Manager
        ScrollableTabRow(
            selectedTabIndex = when(activeTab) {
                "portfolio" -> 0
                "albums" -> 1
                "creative_suite" -> 2
                "growth_intelligence" -> 3
                "settings" -> 4
                else -> 0
            },
            containerColor = Color(0xFF0F1017),
            contentColor = Color(0xFFEC4899),
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when(activeTab) {
                        "portfolio" -> 0
                        "albums" -> 1
                        "creative_suite" -> 2
                        "growth_intelligence" -> 3
                        "settings" -> 4
                        else -> 0
                    }]),
                    color = Color(0xFFEC4899),
                    height = 2.dp
                )
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Tab(
                selected = activeTab == "portfolio",
                onClick = { activeTab = "portfolio" },
                text = { Text("Visual Grid", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "albums",
                onClick = { activeTab = "albums" },
                text = { Text("Folders / Albums", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "creative_suite",
                onClick = { activeTab = "creative_suite" },
                text = { Text("Creative Suite", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "growth_intelligence",
                onClick = { activeTab = "growth_intelligence" },
                text = { Text("Growth Intel", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "settings",
                onClick = { activeTab = "settings" },
                text = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. MAIN TAB CONTENT PANELS
        when (activeTab) {
            "portfolio" -> {
                // VISUAL GRID OF ALL PORTFOLIO CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Portfolio Media",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Button(
                            onClick = { showUploadMediaDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Content", fontSize = 12.sp)
                        }
                    }

                    if (mediaItems.isEmpty()) {
                        // Polished Empty State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF11121B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEC4899).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Your Portfolio is Empty",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Upload premium foot art, behind-the-scenes heels catalogs, or videos. Lock them behind specific coin pricing or make them free to active subscribers!",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { showUploadMediaDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Upload First Content")
                                }
                            }
                        }
                    } else {
                        // Visual grid of items
                        // Composing manually using Rows or a flexible grid since verticalScroll has constraints
                        val chunks = mediaItems.chunked(2)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunks.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.85f)
                                        ) {
                                            PortfolioMediaGridCard(
                                                item = item,
                                                onTap = { selectedMediaPreview = item },
                                                onDelete = { viewModel.deleteCreatorMediaItem(item.id) }
                                            )
                                        }
                                    }
                                    if (rowItems.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "albums" -> {
                // ALBUMS / FOLDERS PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Portfolio Folders / Albums",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Button(
                            onClick = { showAddAlbumDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Folder", fontSize = 12.sp)
                        }
                    }

                    if (albums.isEmpty()) {
                        // Polished Empty Folders State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF11121B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "No Albums Configured",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Create priced premium folders like 'Exclusive Satin Boots' or 'Summer Pedicures' to group your photos. Users can purchase the whole folder to unlock all contents!",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { showAddAlbumDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Create Your First Folder")
                                }
                            }
                        }
                    } else {
                        // List of folders/albums
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            albums.forEach { album ->
                                val albumCount = mediaItems.count { it.albumId == album.id }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                        .clickable { selectedAlbumDetail = album },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Folder Cover
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1E2030)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val imageRes = if (album.coverRes.isNotEmpty()) album.coverRes else "img_purple_bg_1782877584581"
                                            Image(
                                                painter = painterResource(id = getDrawableIdByName(imageRes)),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (album.description.isBlank()) "No description provided." else album.description,
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$albumCount Content(s)",
                                                        color = Color(0xFFA78BFA),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFD4AF37).copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (album.price > 0.0) "${album.price} Coins" else "Free / Sub-Only",
                                                        color = Color(0xFFFBBF24),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // Actions
                                        IconButton(
                                            onClick = { viewModel.deleteCreatorAlbum(album.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Album",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "settings" -> {
                // PROFILE CUSTOMIZATION SETTINGS
                var editName by remember(account) { mutableStateOf(account?.username ?: "") }
                var editBio by remember(account) { mutableStateOf(account?.bio ?: "") }
                var editSubPrice by remember(account) { mutableStateOf(account?.subPrice?.toString() ?: "9.99") }
                var editAvatar by remember(account) { mutableStateOf(account?.avatarRes ?: "img_app_icon_1781222852965") }
                var editBanner by remember(account) { mutableStateOf(account?.bannerRes ?: "img_beach_shower_1783035001001") }
                var editBg by remember(account) { mutableStateOf(account?.backgroundRes ?: "luxury_trio_bg_v2_1783031169841") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Customize Profile Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFEC4899)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Short Bio") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFEC4899)
                        ),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    OutlinedTextField(
                        value = editSubPrice,
                        onValueChange = { editSubPrice = it },
                        label = { Text("Monthly Subscription (Coins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFEC4899)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Profile Image Preset selector
                    Text("Select Profile Avatar Icon:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aestheticPresets.forEach { (res, desc) ->
                            val isSelected = editAvatar == res
                            Card(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable { editAvatar = res }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(res)),
                                        contentDescription = desc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Profile Cover Image Preset selector
                    Text("Select Profile Cover Photo (Banner):", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aestheticPresets.forEach { (res, desc) ->
                            val isSelected = editBanner == res
                            Card(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable { editBanner = res }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(res)),
                                        contentDescription = desc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Background Image Preset selector
                    Text("Select App Background Image (Global Theme):", fontSize = 12.sp, color = Color.Gray)
                    val backgroundPresets = listOf(
                        "luxury_trio_bg_v2_1783031169841" to "SoleLuxe Signature Trio (9:16 Port.)",
                        "luxury_trio_bg_1783004590296" to "SoleLuxe Original Signature",
                        "img_beach_shower_1783035001001" to "Emerald Coast Shower (Turquoise)",
                        "img_speedboat_duo_1783035002002" to "Yacht Club Sunsets (Orange/Fuchsia)",
                        "img_dancer_motion_1783035003003" to "Ballroom Rhythm Motion (Violet)",
                        "img_purple_bg_1782877584581" to "Luxury Cosmic Purple Glow",
                        "img_open_toes_pink_1782962118574" to "Anastasia Rose Pink Sandals",
                        "img_model2_pedicure_1781222811962" to "Sasha Blue Satin Pedicure",
                        "img_model3_anklet_1781222826340" to "Elena Wilde Summer Anklet",
                        "img_sexy_slippers_yellow_1782962148817" to "Clara Yellow Feather Slippers"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backgroundPresets.forEach { (res, desc) ->
                            val isSelected = editBg == res
                            Card(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable { editBg = res }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(res)),
                                        contentDescription = desc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val priceParsed = editSubPrice.toDoubleOrNull() ?: 9.99
                            if (priceParsed < 0.0) {
                                Toast.makeText(context, "Price cannot be negative!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateCreatorProfile(
                                    name = editName,
                                    bio = editBio,
                                    subPrice = priceParsed,
                                    avatarRes = editAvatar,
                                    bannerRes = editBanner,
                                    backgroundRes = editBg,
                                    context = context
                                )
                                Toast.makeText(context, "Profile settings updated!", Toast.LENGTH_SHORT).show()
                                activeTab = "portfolio"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Custom Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            "creative_suite" -> {
                val scope = rememberCoroutineScope()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("In-App Creative Studio Suite", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Apply automated AI Editorial filters & AR Try-Ons", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6))
                    }

                    // 1. LIVE GRAPHICAL STUDIO PREVIEW CANVAS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF11121A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Live styled image preview (Style Transfer Matrix)
                            Image(
                                painter = painterResource(id = getDrawableIdByName(csSelectedVisual)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix(AiEditorialEngine.getColorMatrixForStyle(csSelectedStyle))),
                                modifier = Modifier.fillMaxSize()
                            )

                            // AR TRY-ON OVERLAY LAYER
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                when (csArOverlayType) {
                                    "ANKLET" -> {
                                        // Draw a shimmering silver chain around the ankle area (curved line)
                                        drawCircle(
                                            color = Color(0xFFE2E8F0),
                                            radius = 8f,
                                            center = Offset(canvasWidth * 0.45f, canvasHeight * 0.40f)
                                        )
                                        // Dynamic chain loop links
                                        for (i in 0..12) {
                                            val t = i.toFloat() / 12f
                                            val x = canvasWidth * (0.35f + t * 0.3f)
                                            val y = canvasHeight * (0.43f + Math.sin(t * Math.PI).toFloat() * 0.08f)
                                            drawCircle(
                                                color = Color(0xFFCBD5E1),
                                                radius = 4f,
                                                center = Offset(x, y)
                                            )
                                            // Charms
                                            if (i == 4 || i == 8) {
                                                drawCircle(
                                                    color = Color(0xFFFCD34D), // Golden drop charms
                                                    radius = 5f,
                                                    center = Offset(x, y + 8f)
                                                )
                                            }
                                        }
                                    }
                                    "STRAPS" -> {
                                        // Draw luxury criss-cross stiletto straps around lower leg and ankle
                                        val strokeWidth = 10f
                                        // Strap 1
                                        drawLine(
                                            color = Color(0xFF0F172A), // Jet black leather
                                            start = Offset(canvasWidth * 0.30f, canvasHeight * 0.20f),
                                            end = Offset(canvasWidth * 0.70f, canvasHeight * 0.55f),
                                            strokeWidth = strokeWidth
                                        )
                                        // Strap 2
                                        drawLine(
                                            color = Color(0xFF0F172A),
                                            start = Offset(canvasWidth * 0.70f, canvasHeight * 0.20f),
                                            end = Offset(canvasWidth * 0.30f, canvasHeight * 0.55f),
                                            strokeWidth = strokeWidth
                                        )
                                        // Gold studs on strap intersections
                                        drawCircle(
                                            color = Color(0xFFF59E0B),
                                            radius = 6f,
                                            center = Offset(canvasWidth * 0.50f, canvasHeight * 0.375f)
                                        )
                                    }
                                    "VOGUE_FRAME" -> {
                                        // High fashion editorial border and text (done via canvas outline + Box overlay text)
                                    }
                                }
                            }

                            // VOGUE MAGAZINE COVER FRAME TEXT OVERLAY
                            if (csArOverlayType == "VOGUE_FRAME") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "SOLELUXE",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            letterSpacing = 6.sp
                                        )
                                        Divider(color = Color.White.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.width(100.dp))
                                        Text(
                                            text = csTextOverlay.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEC4899),
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Column {
                                        Text(
                                            text = "AUTUMN LUXE EDITORIAL",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Style Transfer: ${csSelectedStyle.displayName}",
                                            fontSize = 9.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }

                            // Status tags
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(csSelectedStyle.displayName, color = Color(0xFFEC4899), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. INPUT PICKER: CHOOSE SOURCE UPLOAD
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Choose Raw Visual Asset:", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aestheticPresets.forEach { (res, desc) ->
                                val isSelected = csSelectedVisual == res
                                Card(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable { csSelectedVisual = res }
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color(0xFF8B5CF6) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(res)),
                                        contentDescription = desc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // 3. EDITORIAL STYLE SELECTOR
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("2. Select AI Style Transfer Mode:", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiEditorialEngine.MagazineStyle.values().forEach { style ->
                                val isSelected = csSelectedStyle == style
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { csSelectedStyle = style },
                                    label = { Text(style.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF161722),
                                        labelColor = Color.Gray
                                    )
                                )
                            }
                        }
                        Text(
                            text = csSelectedStyle.quote,
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 4. AR TRY-ON OPTIONS
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Virtual AR Try-On Overlay:", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "NONE" to "None",
                                "STRAPS" to "Leather Straps",
                                "ANKLET" to "Silver Anklet",
                                "VOGUE_FRAME" to "Magazine Cover"
                            ).forEach { (type, label) ->
                                val isSelected = csArOverlayType == type
                                OutlinedButton(
                                    onClick = { csArOverlayType = type },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color(0xFFEC4899).copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSelected) Color(0xFFEC4899) else Color.LightGray
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFEC4899) else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (csArOverlayType == "VOGUE_FRAME") {
                        OutlinedTextField(
                            value = csTextOverlay,
                            onValueChange = { csTextOverlay = it },
                            label = { Text("Magazine Issue Tagline", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFFEC4899)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 5. IN-APP VIDEO TRIMMER (FFMPEG SIMULATOR)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("4. In-App Video Trimmer range", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${String.format("%.1f", csTrimStart)}s - ${String.format("%.1f", csTrimEnd)}s", fontSize = 11.sp, color = Color(0xFFEC4899))
                            }

                            Slider(
                                value = csTrimEnd,
                                onValueChange = { csTrimEnd = it },
                                valueRange = 3f..15f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEC4899),
                                    activeTrackColor = Color(0xFFEC4899)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Save as Pre-Order item?", fontSize = 11.sp, color = Color.LightGray)
                                Switch(
                                    checked = csIsPreOrder,
                                    onCheckedChange = { csIsPreOrder = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEC4899),
                                        checkedTrackColor = Color(0xFFEC4899).copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    // 6. PROCESSING STATE CONSOLE
                    if (csIsProcessing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("FFmpeg Processing Pipeline", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                    Text("${csProgress.toInt()}%", fontSize = 11.sp, color = Color(0xFF34D399))
                                }
                                LinearProgressIndicator(
                                    progress = csProgress / 100f,
                                    color = Color(0xFF34D399),
                                    trackColor = Color.DarkGray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Scrolling logs
                                val scrollState = rememberScrollState()
                                LaunchedEffect(csLogs.size) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(scrollState)
                                ) {
                                    csLogs.forEach { log ->
                                        Text(log, fontSize = 9.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }

                    // Action trigger
                    Button(
                        onClick = {
                            if (!csIsProcessing) {
                                csIsProcessing = true
                                csProgress = 0f
                                csLogs.clear()
                                scope.launch {
                                    csLogs.add("[ffmpeg] initializing studio processing framework...")
                                    delay(400)
                                    csProgress = 15f
                                    csLogs.add("[ffmpeg] parsing raw input source: $csSelectedVisual")
                                    delay(500)
                                    csProgress = 35f
                                    csLogs.add("[ffmpeg] generating style-transfer matrix... LUT: ${csSelectedStyle.displayName}")
                                    delay(600)
                                    csProgress = 60f
                                    csLogs.add("[ffmpeg] rendering AR Try-On vector overlays... Mode: $csArOverlayType")
                                    delay(500)
                                    csProgress = 80f
                                    csLogs.add("[ffmpeg] trimming video keyframes: ${String.format("%.1f", csTrimStart)}s to ${String.format("%.1f", csTrimEnd)}s")
                                    delay(400)
                                    csProgress = 95f
                                    csLogs.add("[ffmpeg] finalizing metadata. PreOrder: $csIsPreOrder")
                                    
                                    val editorialCaption = AiEditorialEngine.applyEditorialStyle(csSelectedStyle, "Fresh studio model pose")
                                    viewModel.addStrategicCreatorMediaItem(
                                        albumId = null,
                                        title = "Studio Edit: ${csSelectedStyle.displayName}",
                                        mediaUri = csSelectedVisual,
                                        mediaType = "PHOTO",
                                        price = 75.0,
                                        collaboratorId = "Sasha",
                                        collaboratorSplit = 30.0,
                                        isPreOrder = csIsPreOrder,
                                        releaseTimestamp = if (csIsPreOrder) System.currentTimeMillis() + 86400000L else 0L,
                                        arOverlayType = if (csArOverlayType == "NONE") null else csArOverlayType,
                                        arOverlayIntensity = 1.0f
                                    )
                                    delay(300)
                                    csProgress = 100f
                                    csLogs.add("[ffmpeg] pipeline execution success! Saved to portfolio database.")
                                    delay(300)
                                    csIsProcessing = false
                                    Toast.makeText(context, "AI Editorial Visual added to portfolio!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        enabled = !csIsProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MovieFilter, contentDescription = null)
                            Text("Burn, Render & Save as Strategic Release", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            "growth_intelligence" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Growth Intelligence Curation", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Actionable analytics, dynamic pricing & joint collab splits", fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981))
                    }

                    // 1. METRICS GRID SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Subscribers", fontSize = 11.sp, color = Color.Gray)
                                Text("142", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("+12% this week", fontSize = 9.sp, color = Color(0xFF10B981))
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Est. Revenue", fontSize = 11.sp, color = Color.Gray)
                                Text("4.2K Coins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                Text("+35% engagement", fontSize = 9.sp, color = Color(0xFF10B981))
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Reach Score", fontSize = 11.sp, color = Color.Gray)
                                Text("94.2%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                Text("Peak hours: 8PM", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }

                    // 2. DYNAMIC PRICING MONITOR
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Demand-Based Smart Dynamic Pricing", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Fluctuates premium item pricing dynamically based on likes flow", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = giEnableDynamicPricing,
                                    onCheckedChange = { giEnableDynamicPricing = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF10B981),
                                        checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.4f)
                                    )
                                )
                            }

                            if (giEnableDynamicPricing) {
                                Divider(color = Color.White.copy(alpha = 0.05f))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Item 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Russian Stiletto Album", fontSize = 12.sp, color = Color.White)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("75.5 Coins", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("(Base: 50.0)", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    // Item 2
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Elena Summer Anklet Loose", fontSize = 12.sp, color = Color.White)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("35.0 Coins", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("(Base: 50.0)", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. COLLABORATIVE ROYALTY SPLIT SMART CONTRACT CALCULATOR
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Collaborative Joint Split Calculator (Royalty API)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            
                            // Creator selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Sasha", "Elena", "Anastasia", "None").forEach { peer ->
                                    val isSelected = (peer == "None" && giCollaboratorId == null) || (giCollaboratorId == peer)
                                    OutlinedButton(
                                        onClick = { giCollaboratorId = if (peer == "None") null else peer },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color.Transparent,
                                            contentColor = if (isSelected) Color(0xFF8B5CF6) else Color.LightGray
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else Color.Gray.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(peer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (giCollaboratorId != null) {
                                // Split Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Partner Royalty Share", fontSize = 11.sp, color = Color.Gray)
                                        Text("${giCollabSplit.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                    }
                                    Slider(
                                        value = giCollabSplit,
                                        onValueChange = { giCollabSplit = it },
                                        valueRange = 10f..90f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF8B5CF6),
                                            activeTrackColor = Color(0xFF8B5CF6)
                                        )
                                    )
                                }

                                val dPrice = giProspectivePrice.toDoubleOrNull() ?: 100.0
                                val splitResult = RoyaltyCalculator.distributeRoyalty(dPrice, "local_creator", giCollaboratorId, giCollabSplit.toDouble())
                                
                                Divider(color = Color.White.copy(alpha = 0.05f))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Content Price", fontSize = 11.sp, color = Color.Gray)
                                        Text("$dPrice Coins", fontSize = 11.sp, color = Color.White)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Platform Fee (20%)", fontSize = 11.sp, color = Color.Gray)
                                        Text("${String.format("%.1f", splitResult.platformFee)} Coins", fontSize = 11.sp, color = Color.White)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("My Net Share", fontSize = 11.sp, color = Color.Gray)
                                        Text("${String.format("%.1f", splitResult.creatorPayout)} Coins", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("$giCollaboratorId Split Share", fontSize = 11.sp, color = Color.Gray)
                                        Text("${String.format("%.1f", splitResult.collaboratorPayout)} Coins", fontSize = 11.sp, color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Royalty Contract Verifier ID: ${splitResult.txHash}",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text("Select a collaborator above to configure dynamic profit splits.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // 4. DEAD-MAN SWITCH SCHEDULER SWITCH
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dead-man Switch API Vault Trigger", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Triggers release events if creator remains inactive for more than set days", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = giEnableDeadManSwitch,
                                    onCheckedChange = { giEnableDeadManSwitch = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEC4899),
                                        checkedTrackColor = Color(0xFFEC4899).copy(alpha = 0.4f)
                                    )
                                )
                            }

                            if (giEnableDeadManSwitch) {
                                Divider(color = Color.White.copy(alpha = 0.05f))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Inactivity Release Limit", fontSize = 11.sp, color = Color.Gray)
                                        Text("${giDeadManDays.toInt()} Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                    }
                                    Slider(
                                        value = giDeadManDays,
                                        onValueChange = { giDeadManDays = it },
                                        valueRange = 7f..90f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFEC4899),
                                            activeTrackColor = Color(0xFFEC4899)
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ WARNING: If you do not publish any updates for ${giDeadManDays.toInt()} days, SoleLuxe will automatically unlock your selected folder vault for free as a gift to your loyal pre-ordered subscribers.",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. ACTIONABLE ALERTS / SUGGESTIONS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                Text("Growth Intelligence Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BulletInsightText("Your reach is down 12% in sheer socks; try posting at 8 PM for 30% higher engagement.")
                                BulletInsightText("Milan High-Noir black and white photography is trending in heels. Create a new Noir album folder next!")
                                BulletInsightText("Dynamic pricing alert: Dynamic adjustments have saved +25.5 coins on high activity items!")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Extra spacing for scroll depth
    }

    // --- DIALOGS & SHEET MODALS ---

    // 1. DIALOG: CREATE ALBUM
    if (showAddAlbumDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("0.0") }
        var selectedCover by remember { mutableStateOf("img_purple_bg_1782877584581") }

        Dialog(onDismissRequest = { showAddAlbumDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Price Group Folder",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Folder Name (e.g. Exclusive Heels)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Brief Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Album Price (0.0 for free/subscribers)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Folder Cover Art Preset:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aestheticPresets.forEach { (res, desc) ->
                            val isSelected = selectedCover == res
                            Card(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable { selectedCover = res }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFF8B5CF6) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = getDrawableIdByName(res)),
                                    contentDescription = desc,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddAlbumDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter a folder name", Toast.LENGTH_SHORT).show()
                                } else {
                                    val priceParsed = price.toDoubleOrNull() ?: 0.0
                                    viewModel.createCreatorAlbum(name, description, priceParsed, selectedCover)
                                    showAddAlbumDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Folder")
                        }
                    }
                }
            }
        }
    }

    // 2. DIALOG: UPLOAD MEDIA
    if (showUploadMediaDialog) {
        var title by remember { mutableStateOf("") }
        var mediaType by remember { mutableStateOf("PHOTO") } // "PHOTO", "VIDEO"
        var selectedAlbumId by remember { mutableStateOf<String?>(null) }
        var price by remember { mutableStateOf("0.0") }
        var selectedMediaRes by remember { mutableStateOf("img_open_toes_pink_1782962118574") }

        Dialog(onDismissRequest = { showUploadMediaDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Upload Premium Content",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Content Title (e.g. Glossy Satin Walk)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFEC4899)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Media Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { mediaType = "PHOTO" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mediaType == "PHOTO") Color(0xFFEC4899) else Color(0xFF1F2030)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PHOTO")
                        }

                        Button(
                            onClick = { mediaType = "VIDEO" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mediaType == "VIDEO") Color(0xFFEC4899) else Color(0xFF1F2030)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VIDEO")
                        }
                    }

                    // Album / Folder Dropdown/Selector
                    if (albums.isNotEmpty()) {
                        Text("Add to Folder (Optional):", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Option: None
                            val isNoneSelected = selectedAlbumId == null
                            Card(
                                modifier = Modifier
                                    .clickable { selectedAlbumId = null }
                                    .border(
                                        width = 2.dp,
                                        color = if (isNoneSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2030)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("[ Loose / No Folder ]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            albums.forEach { album ->
                                val isSelected = selectedAlbumId == album.id
                                Card(
                                    modifier = Modifier
                                        .clickable { selectedAlbumId = album.id }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2030)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                                        Text(album.name, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Individual Item Unlock Price (0.0 for free)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFEC4899)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Simulate Elegant Photo Art Preset:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aestheticPresets.forEach { (res, desc) ->
                            val isSelected = selectedMediaRes == res
                            Card(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable { selectedMediaRes = res }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color(0xFFEC4899) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = getDrawableIdByName(res)),
                                    contentDescription = desc,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showUploadMediaDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                } else {
                                    val priceParsed = price.toDoubleOrNull() ?: 0.0
                                    viewModel.addCreatorMediaItem(
                                        albumId = selectedAlbumId,
                                        title = title,
                                        mediaUri = selectedMediaRes,
                                        mediaType = mediaType,
                                        price = priceParsed
                                    )
                                    showUploadMediaDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Post Media")
                        }
                    }
                }
            }
        }
    }

    // 3. DIALOG: MEDIA PREVIEW
    if (selectedMediaPreview != null) {
        val item = selectedMediaPreview!!
        Dialog(onDismissRequest = { selectedMediaPreview = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = getDrawableIdByName(item.mediaUri)),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Media Tag
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (item.mediaType == "VIDEO") Icons.Default.PlayArrow else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(item.mediaType, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                            Text(
                                text = if (item.price > 0) "${item.price} Coins Unlock Fee" else "Free to active subscribers",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.price > 0) Color(0xFFFBBF24) else Color(0xFF10B981)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.deleteCreatorMediaItem(item.id)
                                selectedMediaPreview = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Delete Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { selectedMediaPreview = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Preview", color = Color.Gray)
                    }
                }
            }
        }
    }

    // 4. DIALOG: ALBUM DETAILS AND MEDIA INSIDE ALBUM
    if (selectedAlbumDetail != null) {
        val album = selectedAlbumDetail!!
        val albumMedia = mediaItems.filter { it.albumId == album.id }

        Dialog(onDismissRequest = { selectedAlbumDetail = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Folder: ${album.name}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (album.description.isBlank()) "No description provided." else album.description,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { selectedAlbumDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.05f))

                    if (albumMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Text("This folder has no media yet", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Media in folder scroll list
                        Column(
                            modifier = Modifier
                                .height(220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            albumMedia.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF161722))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(item.mediaUri)),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${item.mediaType} • ${if (item.price > 0) "${item.price} Coins" else "Free"}", color = Color.Gray, fontSize = 11.sp)
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteCreatorMediaItem(item.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete from Album", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showUploadMediaDialog = true
                            selectedAlbumDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Media directly to Folder")
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioMediaGridCard(
    item: CreatorMediaItem,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() }
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11121A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = getDrawableIdByName(item.mediaUri)),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom semi-transparent gradient plate for title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (item.price > 0.0) "${item.price} Coins" else "Free",
                            color = if (item.price > 0.0) Color(0xFFFBBF24) else Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (item.mediaType == "VIDEO") Icons.Default.PlayArrow else Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Quick Delete tag on Top Left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun BulletInsightText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = Color(0xFFEC4899), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
