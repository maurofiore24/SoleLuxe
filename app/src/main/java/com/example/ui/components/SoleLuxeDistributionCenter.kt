package com.example.ui.components

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.R
import com.example.glassyCard
import com.example.service.SecureLogger
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SoleLuxeDistributionCenter(
    viewModel: MainViewModel,
    lazyListState: androidx.compose.foundation.lazy.LazyListState? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Screen Sub-tab: APK (Direct Native) vs PWA (Progressive Web App)
    var distTypeTab by remember { mutableStateOf("apk") }

    LaunchedEffect(distTypeTab) {
        lazyListState?.animateScrollToItem(0)
    }

    // Dynamic Sideload update simulator state
    var isCheckingByUpdateServer by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf<UpdatePayload?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadOutputMessage by remember { mutableStateOf("") }

    // unknown sources installation status verification
    var isInstallPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        )
    }

    // Interactive step-by-step accordion index state
    var expandedAccordionStep by remember { mutableIntStateOf(1) }

    // Lifecycle effect to keep verifying security parameters
    LaunchedEffect(Unit) {
        while (true) {
            isInstallPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
            delay(1500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Contrast Banner: Commission Bypassing & Distribution Philosophy
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0F111A), Color(0xFF05060A))))
                .border(0.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Independent distribution",
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SoleLuxe Distribution Hub",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bypassing conventional app stores to eliminate the 30% Google & Apple tax corridors. Distribution is 100% direct, censorship-proof, and fully owned by our ecosystem.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }

        // Sub Selector Tab (APK vs PWA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            listOf(
                "apk" to "Direct APK (Native 🤖)",
                "pwa" to "Progressive Web (PWA 🌐)"
            ).forEach { (tabId, label) ->
                val isSelected = distTypeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF7C3AED) else Color.Transparent)
                        .clickable { distTypeTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Gray
                    )
                }
            }
        }

        if (distTypeTab == "apk") {
            // Direct APK Column
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Current Package Version Signature Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ACTIVE CLIENT SIGNATURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RELEASE PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Version Name", fontSize = 11.sp, color = Color.LightGray)
                                Text("v1.2.0-Gold", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Build Package Code", fontSize = 11.sp, color = Color.LightGray)
                                Text("Release Build: #284", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB800))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SHA256: 7e738afbeb08ea6b8c801e06fa6b780df691aefc483a991823eb9af0cc63b2e5",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }

                // In-App Direct Auto Update Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("IN-APP CONTINUOUS AUTOMATED UPDATER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ensures users always run the most performant client binary file without requiring third-party manual search interventions.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger Update Check Button
                        Button(
                            onClick = {
                                if (isCheckingByUpdateServer) return@Button
                                isCheckingByUpdateServer = true
                                updateAvailable = null
                                coroutineScope.launch {
                                    SecureLogger.d("Updater", "Querying distribution nodes for newer client packages")
                                    delay(1200) // Simulating network handshake bound
                                    isCheckingByUpdateServer = false
                                    updateAvailable = UpdatePayload(
                                        versionName = "v1.3.0 Platinum",
                                        buildCode = 302,
                                        changelog = listOf(
                                            "15% Creator High Flow Pool Enabled ✅",
                                            "Dynamic FPS scheduling algorithm updated ⚡",
                                            "R8 Obfuscation & Proguard Rules enforced 🛡️",
                                            "Interactive Secure Media download caches 📂"
                                        ),
                                        apkDownloadUrl = "https://example.com/assets/releases/soleluxe-latest-v130.apk"
                                    )
                                    SecureLogger.d("Updater", "Response completed. New version found: v1.3.0 Platinum")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isCheckingByUpdateServer) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Querying secure servers...", fontSize = 12.sp, color = Color.White)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Check", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ping Update Node Server", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        // Display update payload if available
                        updateAvailable?.let { payload ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("NEW VERSION PENDING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                            }
                                            Text(payload.versionName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB800).copy(alpha = 0.15f))
                                        ) {
                                            Text(
                                                "Build $payload.buildCode",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFB800)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Changelog & Improvements:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    payload.changelog.forEach { bullet ->
                                        Text(" • $bullet", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(vertical = 1.dp))
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Dynamic download simulation action
                                    if (isDownloadingUpdate) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                                color = Color(0xFFEC4899),
                                                trackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Transfer progress: ${(downloadProgress * 100).toInt()}%", fontSize = 10.sp, color = Color.LightGray)
                                                Text("Speed: ~4.8 MB/s", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                            if (downloadOutputMessage.isNotEmpty()) {
                                                Text(
                                                    text = downloadOutputMessage,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF22C55E),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                isDownloadingUpdate = true
                                                downloadProgress = 0f
                                                downloadOutputMessage = "Starting download manager transaction..."
                                                coroutineScope.launch {
                                                    // Simulating the actual incremental download blocks safely
                                                    for (p in 1..20) {
                                                        delay(150L)
                                                        downloadProgress = p / 20f
                                                        if (p == 5) downloadOutputMessage = "📂 Allocated partition buffer /cache/soleluxe-v130.apk"
                                                        if (p == 12) downloadOutputMessage = "📡 Verified download hash block signatures..."
                                                        if (p == 18) downloadOutputMessage = "🔐 Securing package verification metadata..."
                                                    }
                                                    downloadOutputMessage = "✅ Package saved! Triggering native installer intent..."
                                                    delay(500)
                                                    
                                                    // Trigger Native Installer flow!
                                                    try {
                                                        // Constructing a simulated APK location file safely inside caches
                                                        val cacheFile = File(context.cacheDir, "soleluxe_mock_update.apk")
                                                        if (!cacheFile.exists()) {
                                                            cacheFile.createNewFile()
                                                            cacheFile.writeText("MOCK_APK_STREAM_DUMMY_HEADER")
                                                        }
                                                        
                                                        val apkUri: Uri = FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            cacheFile
                                                        )

                                                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(installIntent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Sideload Intent Fired! Package installer dialog launched.", Toast.LENGTH_LONG).show()
                                                    }
                                                    isDownloadingUpdate = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.DownloadForOffline, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sideload Update Packages Directly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        SecureAccessProtocolNotice()
                    }
                }

                // Interactive Security Walkthrough: Android Unknown Sources Guide
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("INSTALLATION & PERMISSION GATEWAYS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isInstallPermissionGranted) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(0.5.dp, if (isInstallPermissionGranted) Color(0xFF22C55E) else Color(0xFFEF4444), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isInstallPermissionGranted) "SECURED AUTHORITY" else "AUTHORIZATION REQUIRED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInstallPermissionGranted) Color(0xFF4ADE80) else Color(0xFFF87171)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To allow independent sideload distribution, Android requires toggling the application's Package Installation Authority inside main settings.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger Native Settings Intent with deep-link target
                        Button(
                            onClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Redirecting to security settings...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInstallPermissionGranted) Color.White.copy(alpha = 0.1f) else Color(0xFF7C3AED)
                            ),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isInstallPermissionGranted) "Revise Security Credentials" else "Grant Unknown Source Authority",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Interactive Steps Walkthrough:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Step-by-Step Accordion Interface
                        listOf(
                            AccordionStep(
                                title = "1. Enable 'Unknown Sources' Sideloading",
                                desc = "Operating systems enforce security gates. Tap 'Grant Unknown Source Authority' button above to land directly on the OS toggle screen, and slide the permission to ALLOW."
                            ),
                            AccordionStep(
                                title = "2. Tap 'Sideload Update Packages'",
                                desc = "Clicking update triggers the secure file transfer protocols. Our background download engine saves the package container into sandboxed download caches under SHA256 integrity audits."
                            ),
                            AccordionStep(
                                title = "3. Resolve Package Installer Intent",
                                desc = "Once downloaded, the standard system Package Installer overlays. Accept the transaction update screen. Your secure local balance data, chats, and customized tokens are kept 100% intact!"
                            )
                        ).forEachIndexed { index, step ->
                            val stepNumber = index + 1
                            val isExpanded = expandedAccordionStep == stepNumber

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { 
                                        expandedAccordionStep = if (isExpanded) 0 else stepNumber 
                                        if (!isExpanded) {
                                            coroutineScope.launch {
                                                delay(100)
                                                lazyListState?.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(step.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(step.desc, fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                CreatorLoungeSection()
                Spacer(modifier = Modifier.height(16.dp))
                CreatorShowcaseSection()
                Spacer(modifier = Modifier.height(16.dp))
                PremiumAccessTierSection()
            }
        } else {
            // PWA Direct Column
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Progressive Web App Layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PROGRESSIVE WEB APP (PWA)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(Icons.Default.Language, contentDescription = "Web", tint = Color(0xFFFFB800), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Standard deployment structure for premium 18+ services. Fully runs in web browsers with lightweight offline resource caching. Instant on-screen launching, completely free from store censorship.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💡 Key Advantages of PWA Model:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("🦖 ", fontSize = 12.sp)
                                    Text("Zero System Permissions: Runs safely within isolated web browser containers.", fontSize = 11.sp, color = Color.LightGray)
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("🚀 ", fontSize = 12.sp)
                                    Text("Instant Launcher Item: Tapping 'Add to Home Screen' creates a direct visual launcher icon.", fontSize = 11.sp, color = Color.LightGray)
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("📦 ", fontSize = 12.sp)
                                    Text("Micro Payload Footprint: Consumes less than 2MB of local package cache memory.", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val pwaUrl = "https://ais-pre-2t3pxac7a6ww7d5r2qf2g7-913594452103.europe-west2.run.app"

                        // Action linking to our shared web endpoint
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pwaUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Shared PWA Deployment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to Copy the link for iPhone Users
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(pwaUrl))
                                Toast.makeText(context, "PWA URL copied! Send this link to your iPhone.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link for iPhone / iOS Friends", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SecureAccessProtocolNotice()
                    }
                }

                // Apple iPhone iOS installation Guide Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFC084FC).copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Apple iOS",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IPHONE & IPAD (iOS) SETUP ASSISTANT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Apple iOS supports PWA standalone launch options. To install SoleLuxe on your iPhone/iPad:",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Step list in Serbian as requested
                        listOf(
                            "1. Kopirajte link iznad (tapnite 'Copy Link for iPhone / iOS Friends') i pošaljite ga na svoj iPhone.",
                            "2. Otvorite taj link isključivo preko SAFARI pretraživača na svom iPhone uređaju.",
                            "3. Dodirnite dugme 'Share' (dugme u obliku kvadrata sa strelicom nagore) na dnu ekrana u Safariju.",
                            "4. Skrolujte nadole i izaberite opciju 'Add to Home Screen' (ili 'Dodaj na početni ekran').",
                            "5. Tapnite na 'Add' (ili 'Dodaj') u gornjem desnom uglu. Aplikacija se pojavljuje direktno na vašem ekranu sa ikonicom!"
                        ).forEach { stepText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = stepText,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Browser Support Matrix Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("COMPATIBILITY MATRIX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Chrome: Complete 🟢", "Safari: Complete 🟢", "Brave: Complete 🟢").forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================================
        // UNIVERSAL PLATFORM ADAPTIVITY & ECOSYSTEM SIMULATOR CONSOLE (Task 1 & Task 3 QA)
        // ====================================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Universal Ecosystem",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ecosystem Responsive Simulator & QA Hub",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Task 1 & 3 QA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interactive verification dashboard demonstrating shared Multiplatform business logic, dynamic Material3 window density adaptation, offline PWA caching, and REST state synchronization.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Controls - Width / Device Selector
                Text(
                    text = "1. Material3 WindowSizeClass Adaptive Layout Preview",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Select simulated display density to observe automatic UI reconfiguration (vertical stack vs. dual-pane split vs. navigation sidebar + grid layout).",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var simulatedDeviceClass by remember { mutableStateOf("mobile") } // "mobile", "tablet", "desktop"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    listOf(
                        "mobile" to "Compact (Mobile 📱)",
                        "tablet" to "Medium (Tablet 📟)",
                        "desktop" to "Expanded (Desktop 💻)"
                     ).forEach { (devId, devLabel) ->
                         val isDevSelected = simulatedDeviceClass == devId
                         Box(
                             modifier = Modifier
                                 .weight(1f)
                                 .clip(RoundedCornerShape(8.dp))
                                 .background(if (isDevSelected) Color(0xFF10B981) else Color.Transparent)
                                 .clickable { simulatedDeviceClass = devId }
                                 .padding(vertical = 8.dp),
                             contentAlignment = Alignment.Center
                         ) {
                             Text(
                                 text = devLabel,
                                 fontSize = 10.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = if (isDevSelected) Color.White else Color.Gray
                             )
                         }
                     }
                 }

                 Spacer(modifier = Modifier.height(12.dp))

                 // Offline cache mode & Low-bandwidth simulation toggle
                 var offlineFirstModeActive by remember { mutableStateOf(false) }
                 val syncTerminalLogs = remember { mutableStateListOf<String>() }
                 var isSyncingInProcess by remember { mutableStateOf(false) }
                 var simulatedPushPayload by remember { mutableStateOf<String?>(null) }

                 // Real PWA simulator Canvas Frame
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(260.dp)
                         .clip(RoundedCornerShape(12.dp))
                         .background(Color(0xFF090A10))
                         .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                         .padding(8.dp)
                 ) {
                     Column(modifier = Modifier.fillMaxSize()) {
                         // Simulated browser/app top status address bar
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                 .padding(horizontal = 8.dp, vertical = 6.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Icon(Icons.Default.Lock, contentDescription = "Secure", tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text(
                                     text = "https://app.soleluxe.com/pwa/feed",
                                     fontSize = 9.sp,
                                     fontFamily = FontFamily.Monospace,
                                     color = Color.LightGray
                                 )
                             }
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(6.dp)
                             ) {
                                 if (offlineFirstModeActive) {
                                     Icon(Icons.Default.WifiOff, contentDescription = "Offline", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                     Text("OFFLINE CACHE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                 } else {
                                     Icon(Icons.Default.Wifi, contentDescription = "Online", tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                     Text("CONNECTED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                 }
                             }
                         }

                         Spacer(modifier = Modifier.height(8.dp))

                         // Simulated standalone PWA iOS notification slide-in overlay inside browser viewport frame
                         AnimatedVisibility(
                             visible = simulatedPushPayload != null,
                             enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                             exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                             modifier = Modifier.padding(bottom = 6.dp)
                         ) {
                             simulatedPushPayload?.let { payload ->
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .clip(RoundedCornerShape(8.dp))
                                         .background(Color(0xFF1F1B2C))
                                         .border(0.5.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                                         .padding(8.dp)
                                 ) {
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         Icon(Icons.Default.NotificationsActive, contentDescription = "Push Notification", tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                                         Spacer(modifier = Modifier.width(8.dp))
                                         Column {
                                             Text("PWA Apple Push Notification 🔔", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                             Text(payload, fontSize = 8.sp, color = Color.LightGray)
                                         }
                                     }
                                 }
                             }
                         }

                         // Render selected layout adaptivity inside the viewport
                         Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                             when (simulatedDeviceClass) {
                                 "mobile" -> {
                                     // Mobile Layout: Single vertical stack
                                     Column(
                                         modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                         verticalArrangement = Arrangement.spacedBy(6.dp)
                                     ) {
                                         Text("Mobile feed stack (Vertical Compact Density)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                         // Feed item
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .background(Color.White.copy(alpha = 0.03f))
                                                 .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                 .padding(8.dp)
                                         ) {
                                             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                 Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                                                 Column {
                                                     Text("Anastasia (@anastasia)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                     Text("Noir Heels Special series. Shared Kotlin logic calculations complete.", fontSize = 8.sp, color = Color.LightGray)
                                                 }
                                             }
                                         }
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .background(Color.White.copy(alpha = 0.03f))
                                                 .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                 .padding(8.dp)
                                         ) {
                                             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                 Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFFB800)))
                                                 Column {
                                                     Text("Sasha (@sasha)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                     Text("Dynamic Pricing active index: 42.5 coins.", fontSize = 8.sp, color = Color.LightGray)
                                                 }
                                             }
                                         }
                                     }
                                 }
                                 "tablet" -> {
                                     // Tablet Layout: Dual pane screen
                                     Row(
                                         modifier = Modifier.fillMaxSize(),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         // Left Pane (List)
                                         Column(
                                             modifier = Modifier.weight(1f).fillMaxHeight(),
                                             verticalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             Text("Left Feed Pane", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                             listOf("Anastasia", "Sasha").forEach { name ->
                                                 Box(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .clip(RoundedCornerShape(6.dp))
                                                         .background(Color.White.copy(alpha = 0.04f))
                                                         .padding(6.dp)
                                                 ) {
                                                     Text(name, fontSize = 9.sp, color = Color.White)
                                                 }
                                             }
                                         }
                                         // Right Pane (Detail)
                                         Box(
                                             modifier = Modifier
                                                 .weight(1.2f)
                                                 .fillMaxHeight()
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .background(Color.White.copy(alpha = 0.02f))
                                                 .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                 .padding(8.dp)
                                         ) {
                                             Column {
                                                 Text("Right Detail Pane", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Text("Selected Creator: Anastasia", fontSize = 8.sp, color = Color.LightGray)
                                                 Text("Pricing Engine calculations: base price ($50.0) adjusted dynamically to $257.5 based on recent fan telemetry.", fontSize = 8.sp, color = Color.LightGray, lineHeight = 11.sp)
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Text("Royalty split: Main Creator (70%) Collab (30%) Platform fee (20%) validated by JVM test suites.", fontSize = 8.sp, color = Color(0xFFFFB800), lineHeight = 11.sp)
                                             }
                                         }
                                     }
                                 }
                                 else -> {
                                     // Desktop Layout: Sidebar + Grid layout
                                     Row(modifier = Modifier.fillMaxSize()) {
                                         // Left Sidebar Nav
                                         Column(
                                             modifier = Modifier
                                                 .width(64.dp)
                                                 .fillMaxHeight()
                                                 .background(Color.White.copy(alpha = 0.03f))
                                                 .padding(4.dp),
                                             verticalArrangement = Arrangement.spacedBy(6.dp),
                                             horizontalAlignment = Alignment.CenterHorizontally
                                         ) {
                                             Text("SIDEBAR", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                             listOf(Icons.Default.Feed, Icons.Default.Analytics, Icons.Default.CloudSync, Icons.Default.Settings).forEach { icon ->
                                                 Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                             }
                                         }
                                         Spacer(modifier = Modifier.width(6.dp))
                                         // Right grid content
                                         Column(modifier = Modifier.weight(1f)) {
                                             Text("Desktop Grid Layout (3-Column adaptivity)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Row(
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                 modifier = Modifier.fillMaxWidth()
                                             ) {
                                                 listOf("Milan Noir", "Rome Velvet", "Paris Satin").forEach { shoeSeries ->
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .height(100.dp)
                                                             .clip(RoundedCornerShape(6.dp))
                                                             .background(Color.White.copy(alpha = 0.04f))
                                                             .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                             .padding(6.dp)
                                                     ) {
                                                         Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                                             Text(shoeSeries, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                             Box(
                                                                 modifier = Modifier
                                                                     .clip(RoundedCornerShape(4.dp))
                                                                     .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                                                             ) {
                                                                 Text("ACTIVE", fontSize = 6.sp, color = Color(0xFF10B981))
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
                 }

                 Spacer(modifier = Modifier.height(12.dp))

                 // Operational controls
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     // Toggle offline caching
                     Button(
                         onClick = {
                             offlineFirstModeActive = !offlineFirstModeActive
                             val statusText = if (offlineFirstModeActive) "Offline caching mode engaged. Bypassing cloud handshakes. Loading low-bandwidth WebP images." else "Connected to shared REST cloud APIs."
                             syncTerminalLogs.add(0, " 📡 [CACHE] $statusText")
                         },
                         colors = ButtonDefaults.buttonColors(
                             containerColor = if (offlineFirstModeActive) Color(0xFFEF4444) else Color.White.copy(alpha = 0.08f)
                         ),
                         shape = RoundedCornerShape(8.dp),
                         modifier = Modifier.weight(1f).height(38.dp)
                     ) {
                         Icon(
                             imageVector = if (offlineFirstModeActive) Icons.Default.WifiOff else Icons.Default.Wifi,
                             contentDescription = "Offline Cache Toggle",
                             tint = Color.White,
                             modifier = Modifier.size(14.dp)
                         )
                         Spacer(modifier = Modifier.width(6.dp))
                         Text(
                             text = if (offlineFirstModeActive) "Disable Offline Mode" else "Simulate Offline Mode",
                             fontSize = 10.sp,
                             color = Color.White
                         )
                     }

                     // Push notifications trigger
                     Button(
                         onClick = {
                             simulatedPushPayload = null
                             coroutineScope.launch {
                                 syncTerminalLogs.add(0, " 🔔 [PUSH] Querying browser service worker notification channel...")
                                 delay(1000)
                                 simulatedPushPayload = "Anastasia (@anastasia) uploaded Milan Noir Heels. Subscribed via iOS browser!"
                                 syncTerminalLogs.add(0, " 📥 [PUSH] Simulated iOS/PWA push notification payload rendered.")
                                 delay(5000)
                                 simulatedPushPayload = null
                             }
                         },
                         colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                         shape = RoundedCornerShape(8.dp),
                         modifier = Modifier.weight(1f).height(38.dp)
                     ) {
                         Icon(Icons.Default.NotificationsActive, contentDescription = "Simulate Push", tint = Color.White, modifier = Modifier.size(14.dp))
                         Spacer(modifier = Modifier.width(6.dp))
                         Text("Trigger PWA Push Alert", fontSize = 10.sp, color = Color.White)
                     }
                 }

                 Spacer(modifier = Modifier.height(10.dp))

                 // Trigger sync button and console
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                         text = "2. 2-Way REST API State Synchronization Engine",
                         fontSize = 11.sp,
                         fontWeight = FontWeight.Bold,
                         color = Color.White
                     )

                     Button(
                         onClick = {
                             if (isSyncingInProcess) return@Button
                             isSyncingInProcess = true
                             syncTerminalLogs.add(0, " 📡 [SYNC] Starting 2-way database handshake...")
                             coroutineScope.launch {
                                 delay(800)
                                 syncTerminalLogs.add(0, " 🗄️ [SYNC] Local Room DB audited. Found 14 local bookmarks, 1 wallet update.")
                                 delay(600)
                                 syncTerminalLogs.add(0, " 🛰️ [SYNC] Transferred payload -> POST https://api.soleluxe.com/v1/sync")
                                 delay(700)
                                 syncTerminalLogs.add(0, " 📥 [SYNC] Server responded. 3 remote creator changes merged into local cache.")
                                 delay(500)
                                 syncTerminalLogs.add(0, " 🟢 [SYNC] Conflict resolution complete. Local and web databases in sync.")
                                 isSyncingInProcess = false
                             }
                         },
                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                         shape = RoundedCornerShape(8.dp),
                         modifier = Modifier.height(28.dp),
                         contentPadding = PaddingValues(horizontal = 10.dp)
                     ) {
                         if (isSyncingInProcess) {
                             CircularProgressIndicator(color = Color.White, modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                         } else {
                             Icon(Icons.Default.CloudSync, contentDescription = "Sync Now", tint = Color.White, modifier = Modifier.size(12.dp))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text("Sync Local & Web DB Now", fontSize = 9.sp, color = Color.White)
                         }
                     }
                 }

                 Spacer(modifier = Modifier.height(6.dp))

                 // Console terminal for sync logs
                 Card(
                     colors = CardDefaults.cardColors(containerColor = Color.Black),
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(90.dp)
                         .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                 ) {
                     Box(modifier = Modifier.padding(6.dp)) {
                         if (syncTerminalLogs.isEmpty()) {
                             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                 Text(
                                     "Sync engine idle. Click 'Sync Local & Web DB Now' or toggle offline mode to record events.",
                                     fontFamily = FontFamily.Monospace,
                                     fontSize = 8.sp,
                                     color = Color.Gray,
                                     textAlign = TextAlign.Center
                                 )
                             }
                         } else {
                             LazyColumn(modifier = Modifier.fillMaxSize()) {
                                 items(syncTerminalLogs) { log ->
                                     Text(
                                         text = log,
                                         fontSize = 8.sp,
                                         fontFamily = FontFamily.Monospace,
                                         color = when {
                                             log.contains("🟢") -> Color(0xFF10B981)
                                             log.contains("📡") -> Color(0xFF38BDF8)
                                             log.contains("CACHE") -> Color(0xFFFFB800)
                                             log.contains("EF4444") || log.contains("OFFLINE") -> Color(0xFFEF4444)
                                             else -> Color.LightGray
                                         },
                                         modifier = Modifier.padding(vertical = 1.dp)
                                     )
                                 }
                             }
                         }
                     }
                 }
             }
         }
     }
 }

data class UpdatePayload(
    val versionName: String,
    val buildCode: Int,
    val changelog: List<String>,
    val apkDownloadUrl: String
)

data class AccordionStep(
    val title: String,
    val desc: String
)

@Composable
fun CreatorLoungeSection() {
    val creators = listOf(
        FeaturedCreator(
            name = "Anastasia Romanov",
            handle = "@anastasia",
            specialty = "High-Arch Heel Designer",
            rating = "4.9",
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
        ),
        FeaturedCreator(
            name = "Sasha Velour",
            handle = "@sasha",
            specialty = "Stiletto & Pedicure Art",
            rating = "4.8",
            gradientColors = listOf(Color(0xFF10B981), Color(0xFF3B82F6))
        ),
        FeaturedCreator(
            name = "Aurelia Thorne",
            handle = "@aurelia",
            specialty = "Golden Pedicure Stylist",
            rating = "5.0",
            gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFEC4899))
        ),
        FeaturedCreator(
            name = "Elena Milano",
            handle = "@elena",
            specialty = "Luxury Footwear Blogger",
            rating = "4.9",
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFFF007F))
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("creator_lounge_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CREATOR LOUNGE",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFDF00),
                letterSpacing = 1.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFDF00),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "FEATURED EXCLUSIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            creators.forEach { creator ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017).copy(alpha = 0.60f)),
                    modifier = Modifier
                        .width(200.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFDF00), // Sparkling Gold
                                    Color(0xFFD4AF37), // Classic Metallic Gold
                                    Color(0xFFAA7C11).copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Elegant Placeholder Image with a stunning rich color gradient and subtle camera/fashion icon representing exclusive portfolio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = creator.gradientColors
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Semi-transparent overlay to make it look highly stylized
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.15f))
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Portfolio Showcase",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "EXCLUSIVES ONLY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.9f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Name, Handle & verified badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = creator.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Verified Member",
                                        tint = Color(0xFFFFDF00),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = creator.handle,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Specialty and rating row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = creator.specialty,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.LightGray,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFDF00),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = creator.rating,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FeaturedCreator(
    val name: String,
    val handle: String,
    val specialty: String,
    val rating: String,
    val gradientColors: List<Color>
)

@Composable
fun SecureAccessProtocolNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017).copy(alpha = 0.50f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFDF00), // Sparkling Gold
                        Color(0xFFD4AF37), // Classic Metallic Gold
                        Color(0xFFAA7C11).copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield Security",
                    tint = Color(0xFFFFDF00),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SECURE ACCESS PROTOCOL (E2EE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFDF00),
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "All fan-to-creator chats, wallet token sync activities, and premium portfolio uploads are fortified with end-to-end encryption. Content is processed using decentralized peer pipelines to maintain total sovereign anonymity and absolute digital security.",
                fontSize = 10.sp,
                color = Color.LightGray,
                lineHeight = 14.sp
            )
        }
    }
}

data class ShowcaseItem(
    val imageResId: Int,
    val title: String,
    val creator: String,
    val badge: String,
    val gradientColors: List<Color>
)

@Composable
fun CreatorShowcaseSection() {
    val showcaseItems = remember {
        listOf(
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_model1_highheels_1781222793764,
                title = "Crimson Eclipse Heels",
                creator = "@anastasia",
                badge = "SNEAK PEEK",
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
            ),
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_model2_pedicure_1781222811962,
                title = "Starlight Foot Art",
                creator = "@sasha",
                badge = "EXCLUSIVE",
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF3B82F6))
            ),
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_model3_anklet_1781222826340,
                title = "24K Gold Pearl Anklet",
                creator = "@aurelia",
                badge = "HIGH ART",
                gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFEC4899))
            ),
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_model4_stockings_1781222837876,
                title = "Midnight Mesh Silk",
                creator = "@elena",
                badge = "PRESTIGE",
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFFF007F))
            ),
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_blue_nails_heels_1782876971256,
                title = "Cerulean Open-Toes",
                creator = "@anastasia",
                badge = "PORTFOLIO",
                gradientColors = listOf(Color(0xFFEC4899), Color(0xFFFBBF24))
            ),
            ShowcaseItem(
                imageResId = com.example.R.drawable.img_red_nails_heels_1782876423202,
                title = "Scarlet Silk Platforms",
                creator = "@sasha",
                badge = "NEW TREND",
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
            )
        )
    }

    var autoHighlightedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            autoHighlightedIndex = (autoHighlightedIndex + 1) % showcaseItems.size
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("creator_showcase_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CREATOR SHOWCASE",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFDF00),
                letterSpacing = 1.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFDF00),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "AUTO-PLAYING GRID",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val chunked = showcaseItems.chunked(2)
            chunked.forEachIndexed { rowIndex, rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEachIndexed { colIndex, item ->
                        val itemIndex = rowIndex * 2 + colIndex
                        val isAutoHighlighted = autoHighlightedIndex == itemIndex
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            ShowcaseGridItem(
                                item = item,
                                index = itemIndex,
                                isAutoHighlighted = isAutoHighlighted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShowcaseGridItem(
    item: ShowcaseItem,
    index: Int,
    isAutoHighlighted: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isUserActive = isHovered || isPressed
    val active = isUserActive || isAutoHighlighted

    val scale by animateFloatAsState(
        targetValue = if (active) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "showcase_item_scale"
    )

    val borderStrokeWidth by animateDpAsState(
        targetValue = if (active) 1.5.dp else 0.5.dp,
        animationSpec = tween(300),
        label = "showcase_border_width"
    )

    val goldBrush = Brush.linearGradient(
        colors = if (active) {
            listOf(
                Color(0xFFFFDF00), // Sparkling Premium Gold
                Color(0xFFD4AF37), // Metallic Gold
                Color(0xFFAA7C11)  // Bronze Gold
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            )
        }
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0D12)),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { /* Interacts with the exclusive preview */ }
            )
            .border(
                width = borderStrokeWidth,
                brush = goldBrush,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (active) Color(0xFFFFDF00) else Color.Black.copy(alpha = 0.7f)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (active) Color.Transparent else Color(0xFFFFDF00).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (active) Color.Black else Color(0xFFFFDF00)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.creator,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (active) Color(0xFFFFDF00) else Color.LightGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Exclusive",
                            tint = Color(0xFFFFDF00),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "PREVIEW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFDF00)
                        )
                    }
                }
            }
        }
    }
}

data class PremiumTier(
    val name: String,
    val price: String,
    val perks: List<String>,
    val badge: String? = null,
    val badgeBgColor: Color = Color.Transparent,
    val badgeTextColor: Color = Color.Black,
    val glowColors: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PremiumAccessTierSection() {
    val context = LocalContext.current
    val tiers = remember {
        listOf(
            PremiumTier(
                name = "BRONZE STARTER",
                price = "$9.99 / MONTH",
                perks = listOf(
                    "Access to 5 premium creator galleries",
                    "Standard HD (1080p) streaming quality",
                    "Basic interactive chat privileges",
                    "Exclusive community forum entry"
                ),
                glowColors = listOf(Color(0xFFCD7F32), Color(0xFF8B5A2B)), // Bronze/Brown
                icon = Icons.Default.WorkspacePremium
            ),
            PremiumTier(
                name = "GOLD VIP ELITE",
                price = "$29.99 / MONTH",
                badge = "MOST POPULAR",
                badgeBgColor = Color(0xFFFFDF00),
                badgeTextColor = Color.Black,
                perks = listOf(
                    "Unlimited access to ALL creator lounges",
                    "Ultra HD 4K high-fidelity streaming",
                    "Direct priority messaging to top stars",
                    "First dibs on daily exclusive sneak peeks",
                    "Special golden username badge"
                ),
                glowColors = listOf(Color(0xFFFFDF00), Color(0xFFD4AF37), Color(0xFFAA7C11)), // Gold
                icon = Icons.Default.Star
            ),
            PremiumTier(
                name = "PLATINUM PATRON",
                price = "$99.99 / MONTH",
                badge = "ULTIMATE ELITE",
                badgeBgColor = Color(0xFFE5E4E2),
                badgeTextColor = Color.Black,
                perks = listOf(
                    "Personalized custom feet art requests",
                    "Ultra HD 8K HDR high-fidelity streaming",
                    "Bi-weekly live 1-on-1 private streams",
                    "All access & elite golden profile badges",
                    "Private Discord VIP Elite mastermind channel"
                ),
                glowColors = listOf(Color(0xFFE5E4E2), Color(0xFFB0C4DE), Color(0xFFEC4899)), // Platinum/Pink
                icon = Icons.Default.WorkspacePremium
            )
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .testTag("premium_access_tiers")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFDF00),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PREMIUM ACCESS TIERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFDF00),
                    letterSpacing = 1.5.sp
                )
            }
            Text(
                text = "Elevate your passion. Unlock pristine interactive feet art, high-fidelity daily showcases, and direct messaging with elite creators.",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )
        }

        // Display individual tier cards
        tiers.forEach { tier ->
            PremiumTierCard(tier = tier, onActivate = {
                Toast.makeText(context, "${tier.name} activation request sent! Processing secure payment.", Toast.LENGTH_LONG).show()
            })
        }
    }
}

@Composable
fun PremiumTierCard(
    tier: PremiumTier,
    onActivate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val active = isHovered || isPressed

    val scale by animateFloatAsState(
        targetValue = if (active) 1.025f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "tier_scale"
    )

    val borderStrokeWidth by animateDpAsState(
        targetValue = if (active) 1.5.dp else 0.75.dp,
        animationSpec = tween(250),
        label = "tier_border_width"
    )

    val borderColors = if (active) tier.glowColors else listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
    val borderBrush = Brush.linearGradient(colors = borderColors)

    val animatedGlowAlpha by animateFloatAsState(
        targetValue = if (active) 0.15f else 0.03f,
        animationSpec = tween(400),
        label = "tier_glow_alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0D12)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onActivate
            )
            .border(
                width = borderStrokeWidth,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Draw a gorgeous subtle backdrop glow that animates
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tier.glowColors.first().copy(alpha = animatedGlowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(this.size.width * 0.85f, this.size.height * 0.15f),
                            radius = this.size.width * 0.8f
                        )
                    )
                }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row with Name, Icon and optional Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(tier.glowColors.first().copy(alpha = 0.15f))
                                .border(0.5.dp, tier.glowColors.first().copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = tier.icon,
                                contentDescription = null,
                                tint = tier.glowColors.first(),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = tier.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    if (tier.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tier.badgeBgColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tier.badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = tier.badgeTextColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Price display with beautiful premium styling
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tier.price.split(" ").first(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = " / " + tier.price.split(" ").last().lowercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Perk items
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tier.perks.forEach { perk ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = tier.glowColors.first().copy(alpha = 0.85f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = perk,
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Beautiful custom activation button
                val activeColors = if (active) {
                    tier.glowColors
                } else {
                    listOf(
                        tier.glowColors.first().copy(alpha = 0.85f),
                        tier.glowColors.first().copy(alpha = 0.55f)
                    )
                }
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(colors = activeColors))
                        .clickable { onActivate() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (tier.name.contains("GOLD") || tier.name.contains("BRONZE")) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ACTIVATE ${tier.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (tier.name.contains("GOLD") || tier.name.contains("BRONZE")) Color.Black else Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}


