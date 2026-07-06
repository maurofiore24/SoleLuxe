package com.example.ui.components

import android.content.Context
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Creator
import com.example.glassyBackground
import com.example.glassyCard
import com.example.getDrawableIdByName
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.viewmodel.MainViewModel
import java.io.Serializable

// --- DUAL ACCOUNT DATA MODEL ---
data class UserAccount(
    val username: String,
    val handle: String,
    val role: String, // "USER" or "CREATOR"
    val subPrice: Double = 9.99, // default sub price in gold coins
    val unlockFee: Double = 4.99, // premium post unlock fee in gold coins
    val balance: Double = 125.0, // initial simulated creator balance
    val avatarRes: String = "img_app_icon_1781222852965",
    val bio: String = "",
    val bannerRes: String = "img_purple_bg_1782877584581",
    val backgroundRes: String = "luxury_trio_bg_v2_1783031169841"
) : Serializable

// --- DUAL ONBOARDING & ACCOUNT CREATION FLOW ---

@Composable
fun DualOnboardingScreen(
    viewModel: MainViewModel,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(step) {
        scrollState.animateScrollTo(0, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }
    
    // Form Inputs
    var nameInput by remember { mutableStateOf("") }
    var handleInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(null) } // "USER" or "CREATOR"
    var bioInput by remember { mutableStateOf("") }
    
    // Live validation messages
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Solid backing container to blend fit boundaries flawlessly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0F))
        )
        // Always draw the beautiful signature trio background for all onboarding steps
        AsyncImage(
            model = getDrawableIdByName("luxury_trio_bg_v2_1783031169841"),
            contentDescription = "Premium Trio Aesthetic Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            alpha = 0.45f
        )
        // Warm and luxurious gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090A0F).copy(alpha = 0.40f),
                            Color(0xFF7C3AED).copy(alpha = 0.15f),
                            Color(0xFFE040FB).copy(alpha = 0.15f),
                            Color(0xFF090A0F).copy(alpha = 0.50f)
                        )
                    )
                )
        )
        if (step != 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassyBackground()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SOLE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "LUXE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFEC4899),
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // Visually stunning gold-gradient badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFDF00), // Sparkling Gold
                                Color(0xFFD4AF37), // Classic Metallic Gold
                                Color(0xFFAA7C11)  // Rich bronze gold
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFFFFDD0).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Verified Elite",
                    tint = Color(0xFF0F1017),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "VERIFIED ELITE CREATOR",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F1017),
                    letterSpacing = 1.sp
                )
            }
            
            Text(
                text = "PREMIUM CREATOR & FAN PORTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Progress Line Indicator
            Row(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicatorItem(stepNumber = 1, isActive = step >= 1, isCompleted = step > 1)
                Divider(modifier = Modifier.weight(1f).height(2.dp), color = if (step > 1) Color(0xFFEC4899) else Color.DarkGray)
                StepIndicatorItem(stepNumber = 2, isActive = step >= 2, isCompleted = step > 2)
                Divider(modifier = Modifier.weight(1f).height(2.dp), color = if (step > 2) Color(0xFFEC4899) else Color.DarkGray)
                StepIndicatorItem(stepNumber = 3, isActive = step >= 3, isCompleted = step > 3)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step contents with animated changes
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "onboarding_step_transitions"
            ) { currentStep ->
                when (currentStep) {
                    1 -> OnboardingWelcomeStep(onNext = { 
                        errorMessage = null
                        step = 2 
                    })
                    
                    2 -> OnboardingRoleStep(
                        selectedRole = selectedRole,
                        onRoleSelect = { selectedRole = it },
                        onBack = { 
                            errorMessage = null
                            step = 1 
                        },
                        onNext = {
                            if (selectedRole == null) {
                                errorMessage = "Please select an account type to proceed"
                            } else {
                                errorMessage = null
                                step = 3
                            }
                        },
                        errorMessage = errorMessage
                    )
                    
                    3 -> OnboardingDetailsStep(
                        selectedRole = selectedRole ?: "USER",
                        name = nameInput,
                        onNameChange = { nameInput = it },
                        handle = handleInput,
                        onHandleChange = { handleInput = it },
                        bio = bioInput,
                        onBioChange = { bioInput = it },
                        onBack = {
                            errorMessage = null
                            step = 2
                        },
                        onComplete = {
                            if (nameInput.trim().isEmpty()) {
                                errorMessage = "Please enter your Display Name"
                            } else if (handleInput.trim().isEmpty()) {
                                errorMessage = "Please enter a unique handle"
                            } else {
                                errorMessage = null
                                var finalHandle = handleInput.trim()
                                if (!finalHandle.startsWith("@")) {
                                    finalHandle = "@$finalHandle"
                                }
                                selectedRole?.let { role ->
                                    viewModel.registerAccount(
                                        username = nameInput.trim(),
                                        handle = finalHandle,
                                        role = role,
                                        bio = bioInput.trim(),
                                        context = context
                                    )
                                    onOnboardingComplete()
                                }
                            }
                        },
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicatorItem(stepNumber: Int, isActive: Boolean, isCompleted: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isCompleted) Color(0xFFEC4899)
                else if (isActive) Color(0xFF7C3AED)
                else Color.DarkGray
            )
    ) {
        if (isCompleted) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Text(stepNumber.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun ModelAestheticShowcase() {
    val items = listOf(
        Triple("Emerald Coast Shower", "img_beach_shower_1783035001001", "HOT & FRESH"),
        Triple("Yacht Club Sunsets", "img_speedboat_duo_1783035002002", "TRENDING"),
        Triple("Ballroom Violet Rhythm", "img_dancer_motion_1783035003003", "EXCLUSIVE"),
        Triple("Ruby Red Pedicure", "img_red_nails_heels_1782876423202", "TRENDING"),
        Triple("Emerald Green Heels", "img_green_open_toes_1782877109709", "POPULAR"),
        Triple("Sapphire Blue Sandals", "img_blue_nails_heels_1782876971256", "EXCLUSIVE"),
        Triple("Sunset Orange Open-Toe", "img_orange_nails_heels_1782876981350", "HOT")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "EXPLORE EXQUISITE STYLES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEC4899),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { (title, imageRes, tag) ->
                Box(
                    modifier = Modifier
                        .width(145.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    AsyncImage(
                        model = getDrawableIdByName(imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Top badge
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .background(
                                color = if (tag == "TRENDING") Color(0xFFEC4899) else Color(0xFF7C3AED),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Bottom info gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberSpotlight() {
    val testimonials = listOf(
        UserAccount(
            username = "Aurelia Gold",
            handle = "@Aurelia_Gold",
            role = "Elite Footwear Stylist",
            bio = "SoleLuxe redefined my creative sovereignty. The prestige is real, and the payouts are instant and seamless."
        ),
        UserAccount(
            username = "Juliet Lux",
            handle = "@Juliet_Lux",
            role = "High-Fashion Creator",
            bio = "A masterfully designed space for pure elegance and aesthetic appreciation. Truly the elite tier of fan connections."
        ),
        UserAccount(
            username = "Vespera Nails",
            handle = "@Vespera_Nails",
            role = "Premium Nail Artist",
            bio = "The digital security hooks and elegant interface set a standard that other platforms simply can't touch."
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEMBER SPOTLIGHT",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFDF00),
                letterSpacing = 1.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFDF00),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "VERIFIED ELITE",
                    fontSize = 8.sp,
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
            testimonials.forEach { testimonial ->
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F1017).copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFDF00), // Sparkling Gold
                                    Color(0xFFD4AF37), // Classic Metallic Gold
                                    Color(0xFFAA7C11).copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "“",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFDF00).copy(alpha = 0.8f),
                                modifier = Modifier.offset(y = (-14).dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFDF00),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = testimonial.bio,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFD4AF37),
                                                Color(0xFF0F1017)
                                            )
                                        )
                                    )
                                    .border(0.5.dp, Color(0xFFFFDF00).copy(alpha = 0.5f), CircleShape)
                            ) {
                                Text(
                                    text = testimonial.username.first().toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = testimonial.username,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Verified Member",
                                        tint = Color(0xFFFFDF00),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Text(
                                    text = "${testimonial.handle} • ${testimonial.role}",
                                    fontSize = 9.sp,
                                    color = Color.LightGray.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeStep(onNext: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F1235).copy(alpha = 0.85f), // rich deep violet-grape
                        Color(0xFF0E041E).copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEC4899), // vibrant hot pink
                        Color(0xFF7C3AED), // royal purple
                        Color(0xFFEC4899).copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Welcome Elite Icon",
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = "Welcome to the Luxury Tier",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Horizontal aesthetic showcase carousel featuring beautiful legs/feet poses!
            ModelAestheticShowcase()

            // Member Spotlight Section with gold-bordered cards
            MemberSpotlight()

            Text(
                text = "Experience a sovereign digital ecosystem built for elite creators, foot/leg fashion designers, and dedicated enthusiasts.\n\nEnjoy smart contract validation, high-fidelity AI-assisted aesthetics, secure payment logging, and direct sub tiers with zero platform friction.",
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_welcome_next"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enter Gateway", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun OnboardingDetailsStep(
    selectedRole: String,
    name: String,
    onNameChange: (String) -> Unit,
    handle: String,
    onHandleChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    errorMessage: String?
) {
    val titleText = if (selectedRole == "CREATOR") "Create Your Creator Profile" else "Create Your Fan Profile"
    val subtitleText = if (selectedRole == "CREATOR") {
        "Establish your premier content brand and set up your luxury style space."
    } else {
        "Establish your digital presence in the luxury circles."
    }
    val bioPlaceholder = if (selectedRole == "CREATOR") {
        "e.g. Specializing in high-arch designer heels, stilettos and custom nails."
    } else {
        "e.g. Dedicated high fashion collector, nail enthusiast, and style connoisseur."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassyCard(cornerRadius = 24, borderAlpha = 0.15f, backgroundAlpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titleText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitleText,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Username input
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Display Name") },
                placeholder = { Text("e.g. Lady Anastasia") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("input_onboard_name")
            )

            // Handle input
            OutlinedTextField(
                value = handle,
                onValueChange = onHandleChange,
                label = { Text("Unique Handle") },
                placeholder = { Text("e.g. @anastasia") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("input_onboard_handle")
            )

            // Bio input
            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                label = { Text("Short bio / style description") },
                placeholder = { Text(bioPlaceholder) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("input_onboard_bio")
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("onboarding_details_back"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("onboarding_details_complete"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Finish Setup", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OnboardingRoleStep(
    selectedRole: String?,
    onRoleSelect: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    errorMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassyCard(cornerRadius = 24, borderAlpha = 0.15f, backgroundAlpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Your Role",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "How would you like to experience SoleLuxe?",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            // USER Role Card
            Card(
                onClick = { onRoleSelect("USER") },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "USER") Color(0xFF7C3AED).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                ),
                border = BorderStroke(
                    width = 2.dp,
                    brush = if (selectedRole == "USER") {
                        Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                    } else {
                        Brush.linearGradient(listOf(Color.DarkGray, Color.DarkGray))
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { onRoleSelect("USER") }.testTag("role_user_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C3AED).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Regular Fan / Subscriber", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Enjoy feeds, chat with elite models, purchase premium unlocks, and customize styles with AuraAI.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }

            // CREATOR Role Card
            Card(
                onClick = { onRoleSelect("CREATOR") },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "CREATOR") Color(0xFFEC4899).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                ),
                border = BorderStroke(
                    width = 2.dp,
                    brush = if (selectedRole == "CREATOR") {
                        Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF7C3AED)))
                    } else {
                        Brush.linearGradient(listOf(Color.DarkGray, Color.DarkGray))
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { onRoleSelect("CREATOR") }.testTag("role_creator_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Premium Content Creator", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Set custom subscription prices, unlock post premiums, track live wallet coin earnings, and publish digital items.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("onboarding_role_next"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}


// --- PREMIUM SIDE NAVIGATION DRAWER CONTENT ---

@Composable
fun PremiumDrawerContent(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val account by viewModel.userAccount.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFF0F1017))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        // Dynamic App Background photo behind the side menu list
        val backgroundResName = account?.backgroundRes ?: "luxury_trio_bg_v2_1783031169841"
        AsyncImage(
            model = getDrawableIdByName(backgroundResName),
            contentDescription = "Drawer Background Theme",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(1.5.dp)
        )
        // Premium Dark Gradient overlay to guarantee perfect contrast and readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1017).copy(alpha = 0.18f),
                            Color(0xFF0B0C10).copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Drawer Header with user profile card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassyCard(cornerRadius = 16, borderAlpha = 0.12f, backgroundAlpha = 0.04f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Avatar Placeholder
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))))
                            .padding(1.5.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF0F1017))
                        ) {
                            Icon(
                                imageVector = if (account?.role == "CREATOR") Icons.Default.Camera else Icons.Default.Person,
                                contentDescription = "Profile icon",
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = account?.username ?: "Guest Fan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = account?.handle ?: "@guest",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Role badge and balance row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (account?.role == "CREATOR") Color(0xFFEC4899).copy(alpha = 0.15f)
                                else Color(0xFF7C3AED).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = account?.role ?: "FAN",
                            color = if (account?.role == "CREATOR") Color(0xFFEC4899) else Color(0xFF8B5CF6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = "Wallet Balance",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = wallet?.let { String.format("$%.2f", it.balance) } ?: "$0.00",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
            }

            // Divider
            Divider(color = Color.White.copy(alpha = 0.05f))

            // Navigation Items List
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DrawerNavItem(
                    icon = Icons.Default.Home,
                    label = "Feeds & Studio",
                    testTag = "drawer_nav_home",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("feed")
                    }
                )

                DrawerNavItem(
                    icon = Icons.Default.TravelExplore,
                    label = "Explore Premium",
                    testTag = "drawer_nav_explore",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("explore")
                    }
                )

                DrawerNavItem(
                    icon = Icons.Default.Mail,
                    label = "VIP Inbox / Messenger",
                    iconColor = Color(0xFFD4AF37),
                    testTag = "drawer_nav_messages",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("messages")
                    }
                )

                if (account?.role == "CREATOR") {
                    DrawerNavItem(
                        icon = Icons.Default.Dashboard,
                        label = "Creator Settings",
                        iconColor = Color(0xFFEC4899),
                        testTag = "drawer_nav_creator",
                        onClick = {
                            onCloseDrawer()
                            onNavigate("creator_dashboard")
                        }
                    )

                    DrawerNavItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = "My Portfolio Studio",
                        iconColor = Color(0xFF8B5CF6),
                        testTag = "drawer_nav_studio",
                        onClick = {
                            onCloseDrawer()
                            onNavigate("studio")
                        }
                    )
                }

                DrawerNavItem(
                    icon = Icons.Default.Bookmark,
                    label = "My Subscriptions",
                    testTag = "drawer_nav_bookmarks",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("bookmarks")
                    }
                )

                DrawerNavItem(
                    icon = Icons.Default.Settings,
                    label = "Account Settings",
                    testTag = "drawer_nav_settings",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("settings")
                    }
                )
                
                DrawerNavItem(
                    icon = Icons.Default.Info,
                    label = "Fintech Manifesto",
                    testTag = "drawer_nav_manifesto",
                    onClick = {
                        onCloseDrawer()
                        onNavigate("about")
                    }
                )
            }
        }

        // Bottom section with Log Out
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Divider(color = Color.White.copy(alpha = 0.05f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onCloseDrawer()
                        viewModel.logoutAccount(context)
                    }
                    .padding(12.dp)
                    .testTag("drawer_logout_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Exit Icon", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Text("Log Out Session", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
    }
}

@Composable
fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    iconColor: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// --- CREATOR SETTINGS & MANAGEMENT PANEL ---

@Composable
fun CreatorDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val account by viewModel.userAccount.collectAsStateWithLifecycle()
    val creators by viewModel.creators.collectAsStateWithLifecycle()
    
    // Sliders state (loaded from model)
    var monthlyPrice by remember(account) { mutableStateOf(account?.subPrice ?: 9.99) }
    var premiumFee by remember(account) { mutableStateOf(account?.unlockFee ?: 4.99) }

    // Dynamic calculations
    val simulatedSubscribers = remember { 184 }
    val estimatedMonthlyRevenue = monthlyPrice * simulatedSubscribers
    val netRevenue = estimatedMonthlyRevenue * 0.80 // 80/20 split

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassyBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Dashboard Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFFEC4899))
            }
            Column {
                Text(
                    text = "CREATOR FINANCIAL HUBSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Monetization Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Stats Row Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Wallet Balance
            Card(
                modifier = Modifier
                    .weight(1f)
                    .glassyCard(cornerRadius = 16, borderAlpha = 0.12f, backgroundAlpha = 0.05f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                        Text("Accrued Balance", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = String.format("$%.2f", account?.balance ?: 0.0),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD4AF37)
                    )
                    Text("Instant payout ready", fontSize = 9.sp, color = Color.LightGray)
                }
            }

            // Card 2: Active fans
            Card(
                modifier = Modifier
                    .weight(1f)
                    .glassyCard(cornerRadius = 16, borderAlpha = 0.12f, backgroundAlpha = 0.05f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                        Text("Active Fans", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = simulatedSubscribers.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text("+14% this month", fontSize = 9.sp, color = Color(0xFF10B981))
                }
            }
        }

        // Section: Pricing Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassyCard(cornerRadius = 24, borderAlpha = 0.15f, backgroundAlpha = 0.04f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Monthly Subscriptions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly price tier", fontSize = 13.sp, color = Color.LightGray)
                    Text(
                        text = String.format("$%.2f Gold", monthlyPrice),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEC4899)
                    )
                }

                Slider(
                    value = monthlyPrice.toFloat(),
                    onValueChange = { monthlyPrice = Math.round(it * 100.0) / 100.0 },
                    valueRange = 1.99f..49.99f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFEC4899),
                        inactiveTrackColor = Color.DarkGray,
                        thumbColor = Color(0xFFEC4899)
                    ),
                    modifier = Modifier.testTag("slider_monthly_sub_price")
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                Text(
                    text = "Configure Premium Unlocks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unlock charge fee per post", fontSize = 13.sp, color = Color.LightGray)
                    Text(
                        text = String.format("$%.2f Gold", premiumFee),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }

                Slider(
                    value = premiumFee.toFloat(),
                    onValueChange = { premiumFee = Math.round(it * 100.0) / 100.0 },
                    valueRange = 0.99f..19.99f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF7C3AED),
                        inactiveTrackColor = Color.DarkGray,
                        thumbColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier.testTag("slider_premium_unlock_fee")
                )

                Button(
                    onClick = {
                        viewModel.updateCreatorPricing(monthlyPrice, premiumFee, context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_creator_pricing_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Broadcast Pricing", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Section: Projected Revenue Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassyCard(cornerRadius = 24, borderAlpha = 0.15f, backgroundAlpha = 0.04f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sovereign Creator Revenue Split",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Unlike traditional centralized content platforms taking up to 40% with hidden fees, SoleLuxe utilizes direct micro-transactions with an open-source 20% platform ledger commission.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gross Sub Projections", fontSize = 12.sp, color = Color.LightGray)
                    Text(String.format("$%.2f Gold", estimatedMonthlyRevenue), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Processing & Gas Ledger Cost (20%)", fontSize = 12.sp, color = Color.LightGray)
                    Text(String.format("-$%.2f Gold", estimatedMonthlyRevenue * 0.20), fontSize = 13.sp, color = Color(0xFFEF4444))
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Net Monthly Take-home (80%)", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("$%.2f Gold", netRevenue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}


// --- ACCOUNT SETTINGS SCREEN ---

@Composable
fun AccountSettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val account by viewModel.userAccount.collectAsStateWithLifecycle()

    var usernameInput by remember(account) { mutableStateOf(account?.username ?: "") }
    var handleInput by remember(account) { mutableStateOf(account?.handle ?: "") }
    var bioInput by remember(account) { mutableStateOf(account?.bio ?: "") }
    var currentRole by remember(account) { mutableStateOf(account?.role ?: "USER") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassyBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Settings Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF7C3AED).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF8B5CF6))
            }
            Column {
                Text(
                    text = "SOLELUXE SYSTEM PORTAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Account Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Section Card: Profile Fields
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassyCard(cornerRadius = 24, borderAlpha = 0.15f, backgroundAlpha = 0.04f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile Properties",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_username")
                )

                OutlinedTextField(
                    value = handleInput,
                    onValueChange = { handleInput = it },
                    label = { Text("Handle") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_handle")
                )

                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("Style Bio") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio")
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Swap Roles Toggle inside Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Profile Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (currentRole == "CREATOR") "Switched to Monetization view" else "Switched to Fan spectator view",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = currentRole == "CREATOR",
                        onCheckedChange = { isCreator ->
                            currentRole = if (isCreator) "CREATOR" else "USER"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFEC4899),
                            checkedTrackColor = Color(0xFFEC4899).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("settings_role_switch")
                    )
                }

                Button(
                    onClick = {
                        if (usernameInput.isNotEmpty() && handleInput.isNotEmpty()) {
                            viewModel.registerAccount(
                                username = usernameInput,
                                handle = handleInput,
                                role = currentRole,
                                bio = bioInput,
                                context = context
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Account Parameters", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}


// --- ACTIVE BOOKMARKS / SUBSCRIPTIONS LIST ---

@Composable
fun BookmarksScreen(viewModel: MainViewModel) {
    val creators by viewModel.creators.collectAsStateWithLifecycle()
    // Find creators that are currently subscribed to
    val activeSubscriptions = remember(creators) {
        creators.filter { it.subScriptionActive }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassyBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bookmarks Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFD4AF37).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFD4AF37))
            }
            Column {
                Text(
                    text = "YOUR ACTIVE CHANNELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "My Subscriptions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (activeSubscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassyCard(cornerRadius = 24, borderAlpha = 0.12f, backgroundAlpha = 0.03f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No Subs Icon",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Your subscription feeds are empty",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Head to 'Explore Premium' to view elite creators and unlock exclusive stiletto art portfolios, nail blogs, and physical digital cards.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.navigateTo("explore") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                    ) {
                        Text("Browse Creators", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeSubscriptions) { creator ->
                    BookmarkCreatorRow(
                        creator = creator,
                        onViewPortfolio = {
                            viewModel.selectCreator(creator.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkCreatorRow(
    creator: Creator,
    onViewPortfolio: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassyCard(cornerRadius = 16, borderAlpha = 0.12f, backgroundAlpha = 0.04f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = creator.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (creator.verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = creator.handle,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = onViewPortfolio,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("view_portfolio_${creator.id}")
            ) {
                Text("View Portfolio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
