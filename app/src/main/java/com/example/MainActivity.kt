package com.example

import android.app.Activity
import android.view.WindowManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Comment
import com.example.data.model.Creator
import com.example.data.model.Post
import com.example.data.model.UserWallet
import com.example.data.repository.SoleRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AiAnalysisState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.ui.viewmodel.PaymentState
import com.example.ui.components.CreatorGridCardSkeleton
import com.example.ui.components.FeedPostCardSkeleton
import com.example.ui.components.ProfileHeaderSkeleton
import com.example.ui.components.shimmerEffect
import com.example.ui.components.LoungeChatTab
import com.example.ui.components.ComposeErrorBoundary
import com.example.ui.components.LocalErrorBoundary
import com.example.ui.components.AnalyticsAndPerformanceDashboard
import com.example.ui.components.PerformanceEngine
import com.example.ui.components.SoleLuxeStressTestCenter
import com.example.ui.components.SoleLuxeDistributionCenter
import com.example.ui.components.CryptoPaymentScreen
import com.example.ui.components.ManifestoOnboardingScreen
import com.example.ui.components.DualOnboardingScreen
import com.example.ui.components.PremiumDrawerContent
import com.example.ui.components.CreatorDashboardScreen
import com.example.ui.components.AccountSettingsScreen
import com.example.ui.components.BookmarksScreen
import com.example.ui.components.CreatorPortfolioManagerScreen
import com.example.ui.components.ChatScreen
import com.example.ui.components.UserAccount
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(SoleRepository(this), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup standard uncaught exception logging to prevent silent thread freezes and ANRs
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MainActivity", "Uncaught system-level exception on thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Scenario 1: Screenshot & Screen-Recording Prevention
        // Protects sensitive client wallet information, keys, and private seeds from hijack tools.
        // NOTE: FLAG_SECURE prevents screen recording. Because the browser streaming emulator
        // uses screen-capturing to show the device screen, keeping this active turns the preview completely black.
        // Toggle this to 'true' for final hardened production APK distribution.
        val enableSecureFlag = false 
        if (enableSecureFlag) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ComposeErrorBoundary(componentName = "SoleLuxe Main Application") {
                    SoleLuxeApp(viewModel)
                }
            }
        }
    }
}

// Helper to look up generated drawable resource dynamically
@Composable
fun getDrawableIdByName(name: String): Int {
    val context = LocalContext.current
    return remember(name) {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) resId else R.drawable.ic_launcher_foreground
    }
}

// --- MAIN APPLICATION LAYOUT ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoleLuxeApp(viewModel: MainViewModel) {
    val currentRoute by viewModel.currentRoute.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val isCreatorMode by viewModel.isCreatorMode.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Premium custom Insufficient Funds / Refuel dialog
    if (paymentState is PaymentState.Error && (paymentState as PaymentState.Error).errorMessage == "Insufficient Funds") {
        AlertDialog(
            onDismissRequest = { viewModel.resetPaymentState() },
            containerColor = Color(0xFF13141F),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refuel Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "You don't have enough Gold Coins to complete this transaction. Would you like to refuel your wallet with $100.00 Gold Coins now?",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.refillWallet()
                        viewModel.resetPaymentState()
                    }
                ) {
                    Text(
                        text = "Refuel $100.00 Gold",
                        color = Color(0xFFEC4899),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.resetPaymentState() }
                ) {
                    Text(text = "Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Handle user transaction messages elegantly using Snackbars
    LaunchedEffect(userMessage) {
        userMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearUserMessage()
            }
        }
    }

    if (!isOnboarded) {
        DualOnboardingScreen(
            viewModel = viewModel,
            onOnboardingComplete = { viewModel.navigateTo("feed") }
        )
    } else {
        val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0F1017),
                    modifier = Modifier.width(300.dp)
                ) {
                    PremiumDrawerContent(
                        viewModel = viewModel,
                        onNavigate = { route -> viewModel.navigateTo(route) },
                        onCloseDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0F)),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = Color(0xFF1E1E28),
                            contentColor = Color.White,
                            actionColor = Color(0xFFEC4899),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                bottomBar = {
                    SoleBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> viewModel.navigateTo(route) },
                        account = userAccount
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                val backgroundResName = when (currentRoute) {
                    "explore", "creator_detail" -> "img_beach_shower_1783035001001"
                    "messages" -> "img_speedboat_duo_1783035002002"
                    "lounge" -> "img_dancer_motion_1783035003003"
                    "studio", "creator_dashboard" -> "img_purple_bg_1782877584581"
                    "aura_ai" -> "img_model3_anklet_1781222826340"
                    "crypto_payment" -> "img_model2_pedicure_1781222811962"
                    "settings", "about", "bookmarks" -> "img_open_toes_pink_1782962118574"
                    else -> userAccount?.backgroundRes ?: "luxury_trio_bg_v2_1783031169841"
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Solid backing container to blend fit boundaries flawlessly
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF090A0F))
                    )

                    // customizable background image scaled & centered beautifully as full screen crop wall with light blur
                    Image(
                        painter = painterResource(id = getDrawableIdByName(backgroundResName)),
                        contentDescription = "Theme Background",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(1.5.dp),
                        alpha = 0.98f
                    )

                    // Glassmorphic overlay with gradient glows
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassyBackground()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Luxury Sticky Top Bar with Menu opening trigger
                            SoleTopBar(
                                wallet = wallet,
                                onRefill = { viewModel.refillWallet() },
                                onAboutClick = { viewModel.navigateTo("about") },
                                onMessagesClick = { viewModel.navigateTo("messages") },
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )

                            // Render active screen with smooth transitions
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentRoute) {
                                    "explore" -> ExploreScreen(viewModel)
                                    "feed" -> FeedScreen(viewModel)
                                    "lounge" -> LoungeScreen(viewModel)
                                    "aura_ai" -> AuraAiScreen(viewModel)
                                    "studio" -> {
                                        if (isCreatorMode) {
                                            CreatorPortfolioManagerScreen(viewModel)
                                        } else {
                                            StudioScreen(viewModel)
                                        }
                                    }
                                    "crypto_payment" -> CryptoPaymentScreen(viewModel)
                                    "messages" -> ChatScreen(viewModel)
                                    "creator_detail" -> ComposeErrorBoundary("Creator Portfolio Screen") {
                                        CreatorDetailScreen(viewModel)
                                    }
                                    "post_detail" -> PostDetailScreen(viewModel)
                                    "about" -> AboutScreen(viewModel)
                                    "settings" -> AccountSettingsScreen(viewModel)
                                    "creator_dashboard" -> CreatorDashboardScreen(viewModel)
                                    "bookmarks" -> BookmarksScreen(viewModel)
                                    else -> FeedScreen(viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TOP LUXURY SNAP BAR ---

@Composable
fun SoleTopBar(
    wallet: UserWallet?,
    onRefill: () -> Unit,
    onAboutClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp).testTag("top_bar_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                    .padding(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "SoleLuxe Emblem",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SOLE",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "LUXE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFEC4899),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        // Wallet Balance Refiller and About action container
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { onRefill() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("refuel_wallet")
            ) {
                Icon(
                    imageVector = Icons.Default.Paid,
                    contentDescription = "Gold Coins",
                    tint = Color(0xFFEC4899),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = wallet?.let { String.format("$%.2f Gold", it.balance) } ?: "$0.00 Gold",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Refuel Coins",
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = { onMessagesClick() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .testTag("top_bar_messages_shortcut")
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = "VIP Inbox Messages",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onAboutClick() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .testTag("about_trigger")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About / Info Manifesto",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- BOTTOM LUXURIOUS MENU ---

@Composable
fun SoleBottomBar(currentRoute: String, onNavigate: (String) -> Unit, account: UserAccount?) {
    val isCreator = account?.role == "CREATOR"
    
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.03f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
            ),
            shape = RoundedCornerShape(0.dp)
        )
    ) {
        val navItems = mutableListOf<Triple<String, String, ImageVector>>()
        
        // 1. Core Platform Tabs
        navItems.add(Triple("explore", "Explore", Icons.Outlined.TravelExplore))
        navItems.add(Triple("feed", "Feed", Icons.Outlined.Dashboard))
        navItems.add(Triple("lounge", "Lounge", Icons.Outlined.AutoAwesome))
        navItems.add(Triple("aura_ai", "AuraAI", Icons.Outlined.Insights))
        navItems.add(Triple("studio", "Studio", Icons.Outlined.PhotoCamera))
        
        // 2. Additional Left Drawer items (excluding settings and about)
        navItems.add(Triple("messages", "Inbox", Icons.Outlined.Mail))
        if (isCreator) {
            navItems.add(Triple("creator_dashboard", "Creator", Icons.Outlined.Settings))
        }
        navItems.add(Triple("bookmarks", "Subs", Icons.Outlined.Bookmark))

        navItems.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route || 
                (route == "explore" && currentRoute == "creator_detail") || 
                (route == "feed" && currentRoute == "post_detail")
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFFEC4899) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = false, // Hides unselected labels to prevent crowding on compact mobile screens
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0x227C3AED)
                ),
                modifier = Modifier.testTag("nav_btn_$route")
            )
        }
    }
}

// --- 1. EXPLORE DIRECTORY SCREEN ---

@Composable
fun ExploreScreen(viewModel: MainViewModel) {
    val creators by viewModel.creators.collectAsStateWithLifecycle()
    val activePosts by viewModel.posts.collectAsStateWithLifecycle()
    val isExploreLoading by viewModel.isExploreLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verified Creators",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Browse exclusive feet & leg models putting aesthetics first.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = { viewModel.simulateExploreLoad() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Simulate data fetch loading",
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isExploreLoading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(6) {
                    CreatorGridCardSkeleton()
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(creators) { creator ->
                    CreatorGridCard(
                        creator = creator,
                        postsCount = activePosts.count { it.creatorId == creator.id },
                        onClick = { viewModel.selectCreator(creator.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorGridCard(creator: Creator, postsCount: Int, onClick: () -> Unit) {
    val imageId = getDrawableIdByName(creator.avatarRes)
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .glassyCard(cornerRadius = 16)
            .clickable { onClick() }
            .testTag("creator_card_${creator.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = creator.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Glassy Verified Badge / Popularity indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99121115))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Top Rating",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${creator.popularityScore}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creator.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Creator",
                        tint = Color(0xFF00A3FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                Text(
                    text = creator.handle,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$postsCount Posts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC084FC) // Light purple-violet matching theme
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AEC4899)) // 10% translucent pink background pill
                            .border(1.dp, Color(0x40EC4899), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format("$%.2f/mo", creator.subPrice),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEC4899)
                        )
                    }
                }
            }
        }
    }
}

// --- 2. ACTIVE FEED SCREEN ---

@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isFeedLoading by viewModel.isFeedLoading.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val prefetchEngine = PerformanceEngine.prefetchScheduler

    var lastIndex by remember { mutableStateOf(0) }
    var lastOffset by remember { mutableStateOf(0) }
    var lastScrollTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentTime = System.currentTimeMillis()
        val timeDelta = (currentTime - lastScrollTime).coerceAtLeast(1L)
        
        val indexDelta = listState.firstVisibleItemIndex - lastIndex
        val offsetDelta = listState.firstVisibleItemScrollOffset - lastOffset
        
        val pixelsMoved = (indexDelta * 450) + offsetDelta
        
        if (pixelsMoved != 0) {
            val speed = ((kotlin.math.abs(pixelsMoved).toFloat() / timeDelta) * 1000).toInt()
            val scrollingDown = pixelsMoved > 0
            
            val activeIds = posts.map { it.id }
            prefetchEngine.recordScrollEvent(
                scrollingDown = scrollingDown,
                speedPxPerSec = speed,
                activePostIds = activeIds
            )
        }
        
        lastIndex = listState.firstVisibleItemIndex
        lastOffset = listState.firstVisibleItemScrollOffset
        lastScrollTime = currentTime
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.02f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aesthetic Feed",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "High-definition catalog styles, pedicure reviews & luxury leg fashion.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.simulateFeedLoad() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Simulate feed data fetch",
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isFeedLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(3) {
                    FeedPostCardSkeleton()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "No Posts Available",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No updates yet. Check back soon or create one in Studio!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(posts) { post ->
                        FeedPostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onUnlockClick = { viewModel.unlockPost(post.id) },
                            onCardClick = { viewModel.selectPost(post.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val imageId = getDrawableIdByName(post.imageResName)
    val avatarId = getDrawableIdByName(post.creatorAvatarRes)

    val scheduler = PerformanceEngine.prefetchScheduler
    val memoryPool = PerformanceEngine.bitmapPool

    DisposableEffect(post.id) {
        memoryPool.allocateAssetIntoViewport(post.id)
        onDispose {
            memoryPool.releaseOutOfViewportAsset(post.id)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glassyCard(cornerRadius = 20)
            .clickable {
                if (post.isUnlocked) {
                    onCardClick()
                }
            }
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = avatarId),
                    contentDescription = post.creatorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFEC4899), CircleShape)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.creatorName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Profile",
                            tint = Color(0xFF00A3FF),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = post.creatorHandle,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                if (post.postType == "PREMIUM") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (post.isUnlocked) Color(0x1910B981) else Color(0x1AEC4899))
                            .border(1.dp, if (post.isUnlocked) Color(0x3310B981) else Color(0x33EC4899), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (post.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Status",
                                tint = if (post.isUnlocked) Color(0xFF34D399) else Color(0xFFF472B6),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (post.isUnlocked) "UNLOCKED" else "PREMIUM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (post.isUnlocked) Color(0xFF34D399) else Color(0xFFF472B6)
                            )
                        }
                    }
                }
            }

            // Caption text
            Text(
                text = post.caption,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            // Post Media Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                if (post.isUnlocked) {
                    // Post details are fully unlocked, displays high fidelity generated style
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = "Media Post",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    val netMetrics = scheduler.computedQualityMetric
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, if (netMetrics.isDownsamplingForced) Color(0xFFF87171) else Color(0xFF34D399), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (netMetrics.isDownsamplingForced) "⚠️ WebP Downsampled (${netMetrics.fileWeightKb}KB)" else "✨ UHD Lossless (~4.8MB)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netMetrics.isDownsamplingForced) Color(0xFFF87171) else Color(0xFF34D399)
                        )
                    }
                } else {
                    // Blur simulation backdrop for exquisite Locked content
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = imageId),
                            contentDescription = "Media Post Locked",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(25.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x90111116))
                        )

                        // Lock detail parameters
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Post Access Locked",
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aura Premium Post Locked",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Support the model to unlock high-definition photo",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                            )
                            Button(
                                onClick = onUnlockClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                    .testTag("unlock_btn_${post.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("Unlock for %.1f Gold Coins", post.unlockPrice),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD4AF37),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Social Action Row (Likes & Comments link)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(8.dp)
                        .testTag("like_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Post",
                        tint = if (post.isLiked) Color(0xFFEC4899) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        color = if (post.isLiked) Color.White else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            if (post.isUnlocked) {
                                onCardClick()
                            }
                        }
                        .padding(8.dp)
                        .testTag("comment_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Post Comments",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// --- 3. CREATOR DIRECT DETAILED PORTFOLIO SCREEN ---

@Composable
fun CreatorDetailScreen(viewModel: MainViewModel) {
    val creators by viewModel.creators.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val selectedCreatorId by viewModel.selectedCreatorId.collectAsStateWithLifecycle()
    val isProfileLoading by viewModel.isProfileLoading.collectAsStateWithLifecycle()
    
    val creator = creators.find { it.id == selectedCreatorId } ?: return
    
    val albumsFlow = remember(creator.id) { viewModel.repository.getAlbumsByCreator(creator.id) }
    val creatorAlbums by albumsFlow.collectAsStateWithLifecycle(emptyList())

    val mediaFlow = remember(creator.id) { viewModel.repository.getAllMediaByCreator(creator.id) }
    val creatorMediaItems by mediaFlow.collectAsStateWithLifecycle(emptyList())
    val creatorPosts = posts.filter { it.creatorId == creator.id }
    val imageId = getDrawableIdByName(creator.avatarRes)
    val bannerId = getDrawableIdByName(creator.bannerRes)

    var customTipAmount by remember { mutableStateOf("") }
    var simulatedCrashTriggered by remember { mutableStateOf(false) }

    val errorBoundary = LocalErrorBoundary.current
    if (simulatedCrashTriggered) {
        LaunchedEffect(Unit) {
            errorBoundary?.invoke(RuntimeException("Simulated programmatic portfolio exception triggered for diagnostics."))
        }
    }

    if (isProfileLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ProfileHeaderSkeleton()
            }
            item {
                Text(
                    text = "Model Portfolio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            items(2) {
                FeedPostCardSkeleton()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // Model Banner image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = bannerId),
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF090A0F)),
                                startY = 200f
                            )
                        )
                )
                
                // Back Button
                IconButton(
                    onClick = { viewModel.navigateTo("explore") },
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = Color.White
                    )
                }
            }
        }

        // Creator Profile Details card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile image avatar overlapping slightly upwards
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = creator.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .offset(y = (-30).dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF090A0F), CircleShape)
                            .border(1.5.dp, Color(0xFFEC4899), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.offset(y = (-10).dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = creator.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Models",
                                tint = Color(0xFF00A3FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = creator.handle,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[TEST DISASTER DIAGNOSTIC]",
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable { simulatedCrashTriggered = true }
                        )
                    }
                }

                // Description and Statistics Block
                Column(modifier = Modifier.offset(y = (-15).dp)) {
                    Text(
                        text = creator.bio,
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Categories tags row with neon style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        creator.categoryTags.split(",").forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x22EC4899))
                                    .border(1.dp, Color(0x33EC4899), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEC4899)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subscription / Join Buttons card with glassmorphism styling
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 16)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Unlock Exclusive Posts",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Subscribers get access to all photos",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                                
                                Text(
                                    text = String.format("$%.2f/mo", creator.subPrice),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (creator.subScriptionActive) {
                                Button(
                                    onClick = { viewModel.subscribe(creator.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .testTag("subscribe_action_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Subscribe",
                                            tint = Color.White,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = "Subscribed (Active Access)",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.subscribe(creator.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(24.dp),
                                        contentPadding = PaddingValues(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF334155))))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                            .testTag("subscribe_action_btn_coins")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stars,
                                                contentDescription = "Coins",
                                                tint = Color(0xFFFFB800),
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = String.format("Pay with Coins (%.1f Coins)", creator.subPrice),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.navigateTo("crypto_payment") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(24.dp),
                                        contentPadding = PaddingValues(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                            .testTag("subscribe_action_btn_crypto")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payment,
                                                contentDescription = "Crypto USDT",
                                                tint = Color.White,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = String.format("⚡ Direct USDT Checkout ($%.2f USDT)", creator.subscriptionPriceUSDT),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tip Jar Section Panel with glassmorphism styling
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 12)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💸 Model Support Tip Jar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Show direct appreciation to boost creator popularity!",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5.0, 10.0, 20.0).forEach { amount ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassyCard(cornerRadius = 8, backgroundAlpha = 0.12f, borderAlpha = 0.2f)
                                            .clickable { viewModel.sendTip(creator.id, amount) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format("+$%.0f Coins", amount),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom tip block input with pink highlights
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customTipAmount,
                                    onValueChange = { customTipAmount = it },
                                    label = { Text("Custom Amount", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFEC4899),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        val amt = customTipAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0) {
                                            viewModel.sendTip(creator.id, amt)
                                            customTipAmount = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Tip", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Premium Albums/Folders Section
        if (creatorAlbums.isNotEmpty()) {
            item {
                Text(
                    text = "Exclusive Folders & Albums",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                )
            }

            items(creatorAlbums) { album ->
                val count = creatorMediaItems.count { it.albumId == album.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Album Cover
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2030))
                        ) {
                            val imgRes = if (album.coverRes.isNotEmpty()) album.coverRes else "img_purple_bg_1782877584581"
                            Image(
                                painter = painterResource(id = getDrawableIdByName(imgRes)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = album.description,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$count Item(s) inside",
                                fontSize = 11.sp,
                                color = Color(0xFFEC4899),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Unlock / View button
                        if (album.isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Unlocked", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.unlockCreatorAlbum(album.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (album.price > 0.0) "${album.price} Coins" else "Free",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Loose Premium Media Grid/Items Section
        val looseMedia = creatorMediaItems.filter { it.albumId == null }
        if (looseMedia.isNotEmpty()) {
            item {
                Text(
                    text = "Exclusive Photo & Video Singles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                )
            }

            items(looseMedia) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Blurred preview if locked
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2030))
                        ) {
                            Image(
                                painter = painterResource(id = getDrawableIdByName(item.mediaUri)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(if (!item.isUnlocked && item.price > 0.0) 15.dp else 0.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Singles • ${item.mediaType}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        if (item.isUnlocked || item.price <= 0.0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Unlocked", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.unlockCreatorMediaItem(item.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${item.price} Coins",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Creator posts portfolio directory header
        item {
            Text(
                text = "Model Portfolio",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            )
        }

        // List creator specific updates in grid or checklist
        if (creatorPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This creator hasn't published any posts yet.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(creatorPosts) { post ->
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    FeedPostCard(
                        post = post,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onUnlockClick = { viewModel.unlockPost(post.id) },
                        onCardClick = { viewModel.selectPost(post.id) }
                    )
                }
            }
        }
    }
    }
}

// --- 4. DETAILS OF THE POST & COMMENTS SCREEN ---

@Composable
fun PostDetailScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val selectedPostId by viewModel.selectedPostId.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle(emptyList())

    val post = posts.find { it.id == selectedPostId } ?: return
    val imageId = getDrawableIdByName(post.imageResName)

    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back toolbar - glassy overlay design
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo("feed") }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back to Feed",
                    tint = Color.White
                )
            }
            Text(
                text = "Post Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // Large presentation image
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = "Post Artwork Details",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                // Description details
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.creatorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = post.creatorHandle,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = post.caption,
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Fan Feedback (${comments.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Post comments listed
            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Be the first feet lover to comment!",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(comments) { comment ->
                    CommentBubble(comment)
                }
            }
        }

        // Add feedback comment text box stickies at footer with glassy styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Add standard fan feedback...", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEC4899),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        viewModel.postComment(post.id, commentText)
                        commentText = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Post Comment",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CommentBubble(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorName.take(1).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fan",
                    fontSize = 10.sp,
                    color = Color(0xFFEC4899),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x22EC4899))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                text = comment.text,
                fontSize = 13.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// --- 5. AURA_AI STYLING CONSULTANT SCREEN ---

@Composable
fun AuraAiScreen(viewModel: MainViewModel) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    
    var userPromptQuery by remember { mutableStateOf("") }
    var selectedConceptDrawable by remember { mutableStateOf("img_open_toes_pink_1782962118574") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            // Elegant AuraAI Header Card - Glassmorphism Styling
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .glassyCard(cornerRadius = 16)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "AuraAI Advisor",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "AuraAI Styling Lounge",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Model analysis & styling logic, powered by Gemini 3.1 Pro High Thinking",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "1. Choose Portfolio Image to analyze",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic horizontal carousel of the pre-loaded image options
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                val demoPostDrawables = listOf(
                    "img_open_toes_pink_1782962118574" to "Open Sandals",
                    "img_model2_pedicure_1781222811962" to "Pedicure",
                    "img_model3_anklet_1781222826340" to "Anklets",
                    "img_sexy_slippers_yellow_1782962148817" to "Sexy Slippers"
                )

                items(demoPostDrawables) { pair ->
                    val drawableName = pair.first
                    val label = pair.second
                    val isSelected = selectedConceptDrawable == drawableName
                    val drawableId = getDrawableIdByName(drawableName)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { selectedConceptDrawable = drawableName }
                    ) {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "2. Add styling question, pedicure idea, or pose context",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = userPromptQuery,
                onValueChange = { userPromptQuery = it },
                label = { Text("What colors match? How is the angle? Accessories?", color = Color.Gray) },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEC4899),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("aura_ai_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            val context = LocalContext.current
            Button(
                onClick = {
                    val resId = context.resources.getIdentifier(selectedConceptDrawable, "drawable", context.packageName)
                    val bitmap = if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                    viewModel.triggerAiStyleCheck(bitmap, if (userPromptQuery.isBlank()) "Analyze this lifestyle foot fashion." else userPromptQuery)
                },
                enabled = aiState != AiAnalysisState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                    .testTag("aura_ai_submit")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "Evaluate",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Begin High-Thinking Evaluation",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Output log results
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI Stylist Evaluation Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .glassyCard(cornerRadius = 12, backgroundAlpha = 0.04f, borderAlpha = 0.12f)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        when (val state = aiState) {
                            is AiAnalysisState.Idle -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Analytics,
                                        contentDescription = "Idle results",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "AuraAI is sleeping. Submit an image to trigger high-fidelity modeling and design logic suggestions.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                            is AiAnalysisState.Loading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFEC4899),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "AuraAI is evaluating the geometry & color pallet with HIGH Thinking mode (Gemini 3.1 Pro)... Please wait",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                            is AiAnalysisState.Success -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Success",
                                            tint = Color.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "AuraAI Review Complete",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Green,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.resetAiState() }) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reset",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    
                                    // Raw text representation with line breaks mapped nicely
                                    Text(
                                        text = state.markdownText,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            is AiAnalysisState.Error -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "AuraAI evaluated with feedback:\n${state.errorMessage}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFEC4899)
                                    )
                                    Button(
                                        onClick = { viewModel.resetAiState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(),
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Retry Evaluation", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 6. MODEL POST SIMULATION STUDIO ---

@Composable
fun StudioScreen(viewModel: MainViewModel) {
    val creators by viewModel.creators.collectAsStateWithLifecycle()

    var selectedCreatorId by remember { mutableStateOf("creator_anastasia") }
    var inputCaption by remember { mutableStateOf("") }
    var selectedImageName by remember { mutableStateOf("img_open_toes_pink_1782962118574") }
    var isPremiumSelected by remember { mutableStateOf(false) }
    var priceTargetString by remember { mutableStateOf("30.0") }
    var activeStudioTab by remember { mutableStateOf("post") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Model post creator simulator",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Simulate active models uploading stylish leg, shoe, or anklet photos dynamically.",
                fontSize = 13.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sub tab bar matching the premium M3 design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabsList = remember {
                    val list = mutableListOf(
                        "post" to "Simulator Post 📤",
                        "performance" to "Telemetry 📊",
                        "distribution" to "APK/PWA 🚀",
                        "manifesto" to "Manifesto 📜"
                    )
                    if (com.example.BuildConfig.DEBUG) {
                        list.add("stress_test" to "Stress Test ⚙️")
                    }
                    list
                }
                tabsList.forEach { (tabId, label) ->
                    val isSelected = activeStudioTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { activeStudioTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                    }
                }
            }
        }

        if (activeStudioTab == "post") {
            item {
                // Select Model Identity
                Text(
                    text = "1. Post as Model",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Grid of creator models with glassy style
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .glassyCard(cornerRadius = 12)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        creators.forEach { creator ->
                            val isChosen = selectedCreatorId == creator.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCreatorId = creator.id }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isChosen,
                                    onClick = { selectedCreatorId = creator.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEC4899))
                                )
                                Text(
                                    text = "${creator.name} (${creator.handle})",
                                    fontSize = 14.sp,
                                    color = if (isChosen) Color.White else Color.Gray,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

        item {
            // Select Stock Photographic asset
            Text(
                text = "2. Select High-Fashion Asset",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                val postAssets = listOf(
                    "img_open_toes_pink_1782962118574" to "Open Sandals",
                    "img_model2_pedicure_1781222811962" to "Art Pedicure",
                    "img_model3_anklet_1781222826340" to "Foot Anklet",
                    "img_sexy_slippers_yellow_1782962148817" to "Sexy Slippers"
                )

                items(postAssets) { pair ->
                    val drawableName = pair.first
                    val label = pair.second
                    val isSelected = selectedImageName == drawableName
                    val drawableId = getDrawableIdByName(drawableName)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { selectedImageName = drawableName }
                    ) {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "3. Post Details",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = inputCaption,
                onValueChange = { inputCaption = it },
                label = { Text("Model caption (Describe footwear, pedicures...)", color = Color.Gray) },
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEC4899),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(bottom = 16.dp)
                    .testTag("studio_caption_input")
            )

            // Price configurations - glassy card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .glassyCard(cornerRadius = 12)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Premium Locked content",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Requires fans to pay to view",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                        
                        Switch(
                            checked = isPremiumSelected,
                            onCheckedChange = { isPremiumSelected = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEC4899)
                            ),
                            modifier = Modifier.testTag("studio_premium_switch")
                        )
                    }

                    if (isPremiumSelected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unlock Price:",
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = priceTargetString,
                                onValueChange = { priceTargetString = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFEC4899),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(52.dp)
                                    .testTag("studio_price_input")
                            )
                            Text(
                                text = "  Coins",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            // Publish Button - neon style gradient brush
            Button(
                onClick = {
                    if (inputCaption.isNotBlank()) {
                        viewModel.uploadSimulatedPost(
                            creatorId = selectedCreatorId,
                            caption = inputCaption,
                            imageName = selectedImageName,
                            postType = if (isPremiumSelected) "PREMIUM" else "FREE",
                            price = priceTargetString.toDoubleOrNull() ?: 15.0
                        )
                        inputCaption = ""
                        viewModel.navigateTo("feed")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                    .testTag("studio_publish_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Publish to Community Feed",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        } else if (activeStudioTab == "performance") {
            item {
                AnalyticsAndPerformanceDashboard()
            }
        } else if (activeStudioTab == "distribution") {
            item {
                SoleLuxeDistributionCenter(viewModel)
            }
        } else if (activeStudioTab == "manifesto") {
            item {
                ManifestoOnboardingScreen()
            }
        } else {
            item {
                SoleLuxeStressTestCenter(viewModel)
            }
        }
    }
}

// --- GLASSMORPHIC CUSTOM UTILITIES ---

fun Modifier.glassyBackground(): Modifier = this.drawBehind {
    // Elegant Pitch Onyx Black background with translucency to let background image shine through
    drawRect(Color(0xFF090A0F).copy(alpha = 0.22f))

    // Glow blob 1: Top-Left Purple (#7C3AED with 18% opacity, representing a large soft background blur)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.22f), Color.Transparent),
            center = Offset(x = -size.width * 0.1f, y = -size.height * 0.05f),
            radius = size.width * 1.1f
        )
    )

    // Glow blob 2: Middle-Right/Bottom-Right Pink (#EC4899 with 15% opacity, simulating light-bleed glow)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFEC4899).copy(alpha = 0.17f), Color.Transparent),
            center = Offset(x = size.width * 1.15f, y = size.height * 0.45f),
            radius = size.width * 1.0f
        )
    )
}

fun Modifier.glassyCard(
    cornerRadius: Int = 24,
    borderAlpha: Float = 0.15f,
    backgroundAlpha: Float = 0.05f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(Color.White.copy(alpha = backgroundAlpha))
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = borderAlpha), Color.White.copy(alpha = borderAlpha * 0.2f))
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// --- MAIN VIRAL LOUNGE SCREEN & ALL TAB MODULES ---

@Composable
fun LoungeScreen(viewModel: MainViewModel) {
    var activeSubTab by remember { mutableStateOf("chat") }
    val footprintPoints by viewModel.footprintPoints.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        // Loyalty Header Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassyCard(cornerRadius = 16, backgroundAlpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFFB800), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Points indicator",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "My Footprint Loyalty",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val levelLabel = when {
                            footprintPoints >= 1000 -> "🏆 Platinum Runway"
                            footprintPoints >= 600 -> "💎 Gold Pedicure VIP"
                            footprintPoints >= 300 -> "✨ Silver Sole"
                            else -> "🎖️ Bronze Step"
                        }
                        Text(
                            text = "$levelLabel ($footprintPoints pts)",
                            fontSize = 11.sp,
                            color = Color(0xFFE9D5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Balance preview
                Text(
                    text = wallet?.let { String.format("$%.2f Gold", it.balance) } ?: "$0.00 Gold",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEC4899),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Horizontal pill items
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val subTabs = listOf(
                "chat" to "Lounge DM 💬",
                "match" to "Sole Match 💖",
                "live" to "Live Session 🎬",
                "challenges" to "Step Battles 🏆",
                "nfts" to "Signed NFTs 💎",
                "loyalty" to "Trophy & Perks ⭐"
            )
            items(subTabs) { (route, title) ->
                val isSelected = activeSubTab == route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { activeSubTab = route }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                "chat" -> ComposeErrorBoundary("Lounge Chat Component") {
                    LoungeChatTab(viewModel)
                }
                "match" -> SoleMatchTab(viewModel)
                "live" -> LiveSessionTab(viewModel)
                "challenges" -> ChallengesTab(viewModel)
                "nfts" -> NftsTab(viewModel)
                "loyalty" -> LoyaltyTab(viewModel)
            }
        }
    }
}

// --- FEATURE 1: SOLE MATCH AI PREFERATOR ---
@Composable
fun SoleMatchTab(viewModel: MainViewModel) {
    val currentNail by viewModel.matchNail.collectAsStateWithLifecycle()
    val currentArch by viewModel.matchArch.collectAsStateWithLifecycle()
    val currentShoe by viewModel.matchShoe.collectAsStateWithLifecycle()
    val currentSkin by viewModel.matchSkin.collectAsStateWithLifecycle()

    var profileIndex by remember { mutableStateOf(0) }

    val nailOptions = listOf("Pastel Pink", "Deep Ruby", "French Tip", "Bare Natural")
    val archOptions = listOf("High Arch", "Medium Flat", "Elegant Curve")
    val shoeOptions = listOf("Designer Stilettos", "Barefoot Soft", "Beach Sandals", "Satin Platform")
    val skinOptions = listOf("Warm Bronze", "Fair Beige", "Porcelain White", "Sun-Kissed Golden")

    val profile = viewModel.rawMatchProfiles[profileIndex % viewModel.rawMatchProfiles.size]
    val imageId = getDrawableIdByName(profile.imageResName)

    // Calculate dynamic compatibility rating based on matching selections!
    val matchRate = remember(currentNail, currentArch, currentShoe, currentSkin, profile) {
        var score = 60
        if (profile.nailStyle == currentNail) score += 10
        if (profile.archType == currentArch) score += 10
        if (profile.shoeType == currentShoe) score += 10
        if (profile.skinTone == currentSkin) score += 10
        score
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 16)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sole Match Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Customize your ideal feet aesthetic parameters, and our AI Dating Engine serves an instant feed match!",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Nails Preference
                    Text("Nail Style:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC084FC))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(nailOptions) { nail ->
                            val isSelected = currentNail == nail
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33EC4899) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.matchNail.value = nail }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(nail, fontSize = 10.sp, color = if (isSelected) Color(0xFFEC4899) else Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Arch Preference
                    Text("Arch Type Height:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC084FC))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(archOptions) { arch ->
                            val isSelected = currentArch == arch
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33EC4899) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.matchArch.value = arch }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(arch, fontSize = 10.sp, color = if (isSelected) Color(0xFFEC4899) else Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Shoe Preference
                    Text("Shoe / Legwear type:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC084FC))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(shoeOptions) { shoe ->
                            val isSelected = currentShoe == shoe
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33EC4899) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.matchShoe.value = shoe }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(shoe, fontSize = 10.sp, color = if (isSelected) Color(0xFFEC4899) else Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Skin tone Preference
                    Text("Skin Tone Style:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC084FC))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(skinOptions) { skin ->
                            val isSelected = currentSkin == skin
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33EC4899) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.matchSkin.value = skin }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(skin, fontSize = 10.sp, color = if (isSelected) Color(0xFFEC4899) else Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        // SWIPE MATCH CARD DECK
        item {
            Text(
                text = "Your Current Match Recommendation",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 20)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        Image(
                            painter = painterResource(id = imageId),
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Compatibility Badge Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔥 $matchRate% AI MATCH",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Avatar overlay info bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Maker",
                                        tint = Color(0xFF00A3FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = profile.handle,
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = profile.bio,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Details grid row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Nails: ${profile.nailStyle}", fontSize = 11.sp, color = Color.Gray)
                                Text("Arch: ${profile.archType}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column {
                                Text("Style: ${profile.shoeType}", fontSize = 11.sp, color = Color.Gray)
                                Text("Tone: ${profile.skinTone}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Swipe Decision Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // PASS Button
                            Button(
                                onClick = { profileIndex += 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Pass", tint = Color.LightGray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pass", color = Color.LightGray, fontSize = 12.sp)
                            }

                            // RATE 10/10 MATCH Button
                            Button(
                                onClick = {
                                    viewModel.earnFootprintPoints(15, "AI Sole Match Swiper Activity")
                                    profileIndex += 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Rate Perfect", tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Match & Rate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- FEATURE 2: LIVE SOLE SESSIONS ARRA ---
@Composable
fun LiveSessionTab(viewModel: MainViewModel) {
    val tippers by viewModel.liveTippers.collectAsStateWithLifecycle()
    val requests by viewModel.liveRequests.collectAsStateWithLifecycle()
    val floatingTips by viewModel.floatingTips.collectAsStateWithLifecycle()
    val isCreatorMode by viewModel.isCreatorMode.collectAsStateWithLifecycle()
    val viewerCount by viewModel.liveViewerCount.collectAsStateWithLifecycle()
    val sessionEarnings by viewModel.sessionEarnings.collectAsStateWithLifecycle()
    val celebrationText by viewModel.celebrationText.collectAsStateWithLifecycle()
    val activePoseId by viewModel.activePoseId.collectAsStateWithLifecycle()

    var creatorInputText by remember { mutableStateOf("") }
    var creatorInputCost by remember { mutableStateOf("40") }

    val activePose = remember(requests, activePoseId) {
        requests.find { it.id == activePoseId }
    }

    LaunchedEffect(celebrationText) {
        if (celebrationText != null) {
            kotlinx.coroutines.delay(6000)
            viewModel.clearCelebration()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode switch dashboard vs spectator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isCreatorMode) Color(0xFFEC4899) else Color.Transparent)
                            .clickable { viewModel.toggleCreatorMode(false) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🎬 View as Fan",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isCreatorMode) Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { viewModel.toggleCreatorMode(true) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "👑 Creator Admin",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active notification celebration banners
        if (celebrationText != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F).copy(alpha = 0.95f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(Color(0xFFFE2C55), Color(0xFFEC4899), Color(0xFF7C3AED))),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("🎉", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LIVE DESIGNER GOAL MET!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFB800),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = celebrationText ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearCelebration() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Stream View Monitor Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 20)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        // High heel modelling camera
                        Image(
                            painter = painterResource(id = getDrawableIdByName("img_open_toes_pink_1782962118574")),
                            contentDescription = "Live Model Video",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top bar inside video: Live badge, status details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live status badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFE2C55))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            // Interactive Viewership stats
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Visibility, "View count", tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${viewerCount} watching", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Floating Reaction Overlay Panel (REAL-TIME tipping animation)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            floatingTips.forEach { tip ->
                                key(tip.id) {
                                    var animY by remember { mutableStateOf(0f) }
                                    var animAlpha by remember { mutableStateOf(1f) }
                                    var animScale by remember { mutableStateOf(1f) }

                                    LaunchedEffect(tip.id) {
                                        if (tip.delayMs > 0) {
                                            kotlinx.coroutines.delay(tip.delayMs)
                                        }
                                        val duration = 1500f
                                        val steps = 30
                                        for (step in 1..steps) {
                                            kotlinx.coroutines.delay((duration / steps).toLong())
                                            val progress = step.toFloat() / steps.toFloat()
                                            animY = -120f * progress
                                            animAlpha = 1f - progress
                                            animScale = 1.0f + progress * 0.5f
                                        }
                                        viewModel.removeFloatingTip(tip.id)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                translationX = (tip.xOffset - 0.5f) * 240f
                                                translationY = animY * 2.5f - 10f
                                                alpha = animAlpha
                                                scaleX = animScale
                                                scaleY = animScale
                                            },
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(tip.colorHex).copy(alpha = 0.35f))
                                                .border(1.dp, Color(tip.colorHex).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(tip.emoji, fontSize = 18.sp)
                                            if (tip.label.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(tip.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Current Modeling Action Active Overlay Banner at bottom of stream
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, "Active pose icon", tint = Color(0xFFFE2C55), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activePose != null) {
                                        "Performing: ${activePose?.description}"
                                    } else {
                                        "Streaming: Anastasia Rose Modeling Heels 👠"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Bottom info stats / tools of the monitor
                    if (isCreatorMode) {
                        // Creator quick controller summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CREATOR PERFOMER STUDIO", fontSize = 8.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                Text("Live earnings: $${sessionEarnings} Coins", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1F2937))
                                    .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("🟢 ON AIR (1080P)", color = Color(0xFFC084FC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Fan tipping interactive quick tray
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interactive Viewer Tip Box", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                Text("Refuels simulated wallet!", fontSize = 9.sp, color = Color.Gray)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10.0, 25.0, 50.0).forEach { amount ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable { viewModel.sendLiveTip(amount) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("💸 +$amount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Tip Coins", fontSize = 9.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Creator Panel vs Fan Panel contents
        if (isCreatorMode) {
            // CREATOR EXCLUSIVE INSTRUCTIONS & QUEUE MANAGEMENT
            item {
                Text(
                    text = "Live Fan Request Queue Manager",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Select active fan modeling poses to perform on stream. Mark completed once executed successfully!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (requests.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 16, backgroundAlpha = 0.04f)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No active fan-directed requests in queue right now. Submit a goal below to invite votes!", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(requests) { req ->
                    val isActive = req.id == activePoseId
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(
                                cornerRadius = 16,
                                backgroundAlpha = if (isActive) 0.12f else 0.06f
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color(0xFF7C3AED) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Bounty: ${req.coinCost} Coins  |  Votes Bump: ${req.votesCount}", fontSize = 10.sp, color = Color.LightGray)
                                }
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF7C3AED).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ON-SCREEN ✓", color = Color(0xFFA78BFA), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.completeRequest(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF10B981)))),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, "Complete pose", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Complete", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.pinRequestAsActive(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues()
                                ) {
                                    Icon(Icons.Default.PlayArrow, "Pin pose", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isActive) "Active ✓" else "Model Pose", fontSize = 10.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.dismissRequest(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(30.dp)
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues()
                                ) {
                                    Icon(Icons.Default.Delete, "Dismiss", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dismiss", fontSize = 10.sp, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }

            // CREATOR ADD NEW GOAL TOOL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .glassyCard(cornerRadius = 16)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("📌 Propose New Live Posing Goal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Creating a modeling target allows fans to bid gold coins directly to vote on it.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 10.dp))

                        OutlinedTextField(
                            value = creatorInputText,
                            onValueChange = { creatorInputText = it },
                            label = { Text("Pose description (e.g., Extreme satin sandal walk)", fontSize = 10.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = creatorInputCost,
                                onValueChange = { creatorInputCost = it },
                                label = { Text("Gold cost threshold", fontSize = 10.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = Color(0xFF7C3AED),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            Button(
                                onClick = {
                                    if (creatorInputText.trim().isNotEmpty()) {
                                        val cost = creatorInputCost.toDoubleOrNull() ?: 40.0
                                        viewModel.createLiveRequest(creatorInputText.trim(), cost)
                                        creatorInputText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Launch Goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

        } else {
            // SPECTATOR / FAN EXCLUSIVE VIEW INTERFACES
            item {
                Text(
                    text = "Live Director Request Queue",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Vote or pay to request specific poses, close-ups, or accessory zooms!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (requests.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 16, backgroundAlpha = 0.04f)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Queue cleared! Switch to Performer Admin to propose new active runway targets.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 16)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            requests.forEach { req ->
                                val isPinned = req.id == activePoseId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isPinned) Color.White.copy(alpha = 0.04f) else Color.Transparent)
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(req.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            if (isPinned) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("• performing", color = Color(0xFFFE2C55), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("Cost: ${req.coinCost} Coins  |  Votes: ${req.votesCount}", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.voteOrAddLiveRequest(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Vote +Bump", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Tipper Leaderboard
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Trophy icon", tint = Color(0xFFFFB800), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live stream Tipper Leaderboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .glassyCard(cornerRadius = 16)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        tippers.forEachIndexed { i, (name, balance) ->
                            val medal = when (i) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "🎖️"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(medal, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = if (name == "You") FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (name == "You") Color(0xFFEC4899) else Color.White
                                    )
                                }
                                Text("$${balance} Coins Sent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- FEATURE 3: STEP CHALLENGES ---
@Composable
fun ChallengesTab(viewModel: MainViewModel) {
    val challengeList by viewModel.challenges.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 16, backgroundAlpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🏆 Weekly Step Challenges",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Creators face off under aesthetic themes. Cast your dynamic vote to unlock promotion views & claim loyalty bonuses!",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        items(challengeList) { chall ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 20)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(chall.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(chall.subtitle, fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                    // Dual side-by-side candidates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Candidate A
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = getDrawableIdByName(chall.candidateAImage)),
                                    contentDescription = chall.candidateAName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(chall.candidateAName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                            Text("${chall.candidateAVotes} Votes", fontSize = 10.sp, color = Color.LightGray)

                            Spacer(modifier = Modifier.height(8.dp))

                            if (chall.userVotedFor == null) {
                                Button(
                                    onClick = { viewModel.voteInChallenge(chall.id, "A") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    contentPadding = PaddingValues()
                                ) {
                                    Text("Vote", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else if (chall.userVotedFor == "A") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1B3B2B))
                                        .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("My Choice ✓", fontSize = 10.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Candidate B
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = getDrawableIdByName(chall.candidateBImage)),
                                    contentDescription = chall.candidateBName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(chall.candidateBName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                            Text("${chall.candidateBVotes} Votes", fontSize = 10.sp, color = Color.LightGray)

                            Spacer(modifier = Modifier.height(8.dp))

                            if (chall.userVotedFor == null) {
                                Button(
                                    onClick = { viewModel.voteInChallenge(chall.id, "B") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    contentPadding = PaddingValues()
                                ) {
                                    Text("Vote", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else if (chall.userVotedFor == "B") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1B3B2B))
                                        .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("My Choice ✓", fontSize = 10.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Progress chart when user has voted
                    if (chall.userVotedFor != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val total = chall.candidateAVotes + chall.candidateBVotes
                        val pctA = (chall.candidateAVotes.toFloat() / total.toFloat() * 100).toInt()
                        val pctB = 100 - pctA

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$pctA% ${chall.candidateAName}", fontSize = 10.sp, color = Color.LightGray)
                                Text("$pctB% ${chall.candidateBName}", fontSize = 10.sp, color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(pctA.toFloat() + 0.1f)
                                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(pctB.toFloat() + 0.1f)
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- FEATURE 4: SOLE NFT PORTFOLIOS ---
@Composable
fun NftsTab(viewModel: MainViewModel) {
    val ownedSet by viewModel.ownedNfts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 16, backgroundAlpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💎 Signed Creator Limited Prints",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Creators mint limited-edition signed portraits. Scarcity + Verified signature, tradeable and showcaseable in user Trophy Cases!",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        items(viewModel.nftCatalog) { nft ->
            val isOwned = ownedSet.contains(nft.id)
            val imageId = getDrawableIdByName(nft.imageResource)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 20)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = imageId),
                            contentDescription = nft.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Rarity label ribbon
                        val rarityColor = when (nft.rarity) {
                            "LEGENDARY" -> Color(0xFFFFB800)
                            "EPIC" -> Color(0xFFA855F7)
                            else -> Color(0xFF3B82F6)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .clip(RoundedCornerShape(bottomEnd = 8.dp))
                                .background(rarityColor)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(nft.rarity, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = nft.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Creator: ${nft.creatorName}", fontSize = 10.sp, color = Color.LightGray)
                        Text(
                            text = if (isOwned) "Limited Print Available (MINTED)" else "Only ${nft.remainingCount} prints left!",
                            fontSize = 9.sp,
                            color = if (nft.remainingCount == 1 && !isOwned) Color(0xFFEC4899) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isOwned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B3B2B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Owned in Trophy Case ✓ 🛡️", fontSize = 10.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.purchaseNft(nft.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                            ) {
                                Text("Mint NFT: $${nft.cost} Coins", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- FEATURE 5: TROPHY CASE AND USER LOYALTY PROFILE ---
@Composable
fun LoyaltyTab(viewModel: MainViewModel) {
    val footprintPoints by viewModel.footprintPoints.collectAsStateWithLifecycle()
    val ownedNfts by viewModel.ownedNfts.collectAsStateWithLifecycle()

    val levelInfo = remember(footprintPoints) {
        when {
            footprintPoints >= 1000 -> Triple("Platinum Runway Hero", 1500, "Extraordinary VIP Superfan badge visible dynamically across all comment sheets.")
            footprintPoints >= 600 -> Triple("Gold Pedicure VIP", 1000, "Unlocks exclusive private model guide, premium access chats, VIP letters.")
            footprintPoints >= 300 -> Triple("Silver Sole Explorer", 600, "Access early designer uploads, special boutique notifications & comment tags.")
            else -> Triple("Bronze Step Enthusiast", 300, "Vote challenges, make matches, comment on posts to earn 15+ coins and bump levels.")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glowing Badge Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 20, backgroundAlpha = 0.08f)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFEC4899), Color(0xFFFFD700))),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFFB800), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "Badge Logo", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    
                    Text(
                        text = levelInfo.first,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text("Total points: $footprintPoints XP", fontSize = 12.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)

                    Text(
                        text = levelInfo.third,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Linear points progress bar
                    val percentage = (footprintPoints.toFloat() / levelInfo.second.toFloat()).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$footprintPoints", fontSize = 9.sp, color = Color.Gray)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percentage)
                                    .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                            )
                        }
                        Text("${levelInfo.second}", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        // TROPHY CASE Showcase of Signed NFTs
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderSpecial, contentDescription = "Trophy icon", tint = Color(0xFFFFB800), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("My Signature NFT Trophy Case", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (ownedNfts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .glassyCard(cornerRadius = 16, backgroundAlpha = 0.04f)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your trophy case is empty! Mint limited signed collector prints in the 'Signed NFTs' tab to display them here in your showcase.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    items(ownedNfts.toList()) { nftId ->
                        val item = viewModel.nftCatalog.find { it.id == nftId }
                        if (item != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .width(130.dp)
                                    .glassyCard(cornerRadius = 12, backgroundAlpha = 0.12f)
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(id = getDrawableIdByName(item.imageResource)),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(item.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Signed: ${item.creatorName}", fontSize = 8.sp, color = Color.LightGray)
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFB800).copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("VERIFIED NFT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB800))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unlocked Privileges Panel
        item {
            Text("Loyalty Tier Level Unlocked Perks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .glassyCard(cornerRadius = 16)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Perk 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (footprintPoints >= 100) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = "Status",
                            tint = if (footprintPoints >= 100) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Exclusive Elena's Anklet Summer Letter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                if (footprintPoints >= 100) "Elena Wilde: 'Thank you for rating high matching scores! Hugs! ❤️'" else "Locked - Requires Level 2 Bronze Step (100+ points)",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    // Perk 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (footprintPoints >= 600) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = "Status",
                            tint = if (footprintPoints >= 600) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sasha's Private Pedicure Quick-Guide PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                if (footprintPoints >= 600) "Unlocks beautiful nail polish selection files & accessories checklists!" else "Locked - Requires Level 4 Gold Pedicure VIP (600+ points)",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    // Perk 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (footprintPoints >= 1000) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = "Status",
                            tint = if (footprintPoints >= 1000) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Anastasia Runway VIP Posing Behind-Scene Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                if (footprintPoints >= 1000) "Unlocks ultra-private detailed video poses with velvet platforms!" else "Locked - Requires Level 5 Platinum Runway (1000+ points)",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- SOVEREIGN FINTECH ABOUT & MANIFESTO SCREEN ---

@Composable
fun AboutScreen(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
    ) {
        // Operational Status badge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sovereign Beta - Operational",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Title displaying the Sovereign Fintech Manifesto
        item {
            Text(
                text = "The Sovereign Fintech Manifesto",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "SOLELUXE SYSTEM ARCHITECTURE PRINCIPLES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEC4899),
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }

        // Pillar Cards
        item {
            ManifestoPillarCard(
                number = "01",
                title = "Sovereignty Above Platform Escrows",
                description = "Arbitrary corporate store guidelines collect intrusive personal diagnostics telemetry and freeze creator payouts without warning. Choosing distributed standalone clients shields your financial operations from centralized surveillance networks."
            )
        }

        item {
            ManifestoPillarCard(
                number = "02",
                title = "The Strategic Play Store Deviation",
                description = "Corporate store monopolies restrict applications from mounting robust, device-level video and screenshot blockades. Distributing of the standalone SoleLuxe package allows us to integrate deep FLAG_SECURE hooks, preventing unauthorized screen-grabs and device recording of exclusive visuals."
            )
        }

        item {
            ManifestoPillarCard(
                number = "03",
                title = "True 80/20 Non-Custodial Dividend Routes",
                description = "Conventional credit processors hold your cash in custody and leech upwards of 30-50%. Built on top of public nodes utilizing the high-speed TRON TRC-20 protocol, we route exactly 80% directly to the creator's settlement wallet and 20% to security maintenance reserves - verifiably, instantly, and immutably."
            )
        }

        // Development Roadmap Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Distributed Architecture Roadmap",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    RoadmapPhaseBlock(
                        phase = "Phase 1: Active",
                        title = "Dynamic Ledger Splitting & Recipient Check",
                        description = "Enforcing automated transaction verifications pair-coded with low-reliability memory recovery queue buffers protecting subscription funds."
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    RoadmapPhaseBlock(
                        phase = "Phase 2: Upcoming",
                        title = "Encrypted Visual Host Backups & Chats",
                        description = "Integrating distributed file replication nodes with metadata-private end-to-end user communication gates."
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    RoadmapPhaseBlock(
                        phase = "Phase 3: Research",
                        title = "Self-Sovereign Customizable Smart Contracts",
                        description = "Deploying on-chain smart rules that empower creators to dictate custom access controls, subscription criteria, and multi-signature backups."
                    )
                }
            }
        }

        // Return button
        item {
            Button(
                onClick = { viewModel.navigateTo("explore") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .testTag("about_back_btn")
            ) {
                Text(text = "← Back to Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ManifestoPillarCard(number: String, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEC4899),
                    modifier = Modifier
                        .background(Color(0xFFEC4899).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.LightGray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RoadmapPhaseBlock(phase: String, title: String, description: String) {
    Column {
        Text(
            text = phase,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEC4899),
            letterSpacing = 1.sp
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 2.0.dp)
        )
        Text(
            text = description,
            fontSize = 11.sp,
            color = Color.Gray,
            lineHeight = 15.sp
        )
    }
}

