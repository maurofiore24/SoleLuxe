package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.Creator
import com.example.data.model.Post
import com.example.data.model.UserWallet
import com.example.data.model.CreatorAlbum
import com.example.data.model.CreatorMediaItem
import java.util.UUID
import com.example.data.repository.SoleRepository
import com.example.data.repository.MessagingRepository
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Participant
import com.example.service.GeminiService
import com.example.ui.components.UserAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AiAnalysisState {
    object Idle : AiAnalysisState
    object Loading : AiAnalysisState
    data class Success(val markdownText: String) : AiAnalysisState
    data class Error(val errorMessage: String) : AiAnalysisState
}

sealed interface PaymentState {
    object Idle : PaymentState
    object Processing : PaymentState
    object Success : PaymentState
    data class Error(val errorMessage: String) : PaymentState
}

// Model representations for 5 viral features
data class LiveRequest(
    val id: String,
    val description: String,
    val coinCost: Double,
    val votesCount: Int
)

data class FloatingTip(
    val id: String,
    val emoji: String,
    val label: String,
    val colorHex: Int,
    val xOffset: Float,
    val delayMs: Long = 0
)

data class StepChallenge(
    val id: String,
    val title: String,
    val subtitle: String,
    val candidateAName: String,
    val candidateBName: String,
    val candidateAAvatar: String,
    val candidateBAvatar: String,
    val candidateAImage: String,
    val candidateBImage: String,
    val candidateAVotes: Int,
    val candidateBVotes: Int,
    val userVotedFor: String? = null // "A" or "B" or null
)

data class MatchProfile(
    val id: String,
    val name: String,
    val handle: String,
    val imageResName: String,
    val archType: String,
    val nailStyle: String,
    val shoeType: String,
    val skinTone: String,
    val bio: String
)

class MainViewModel(val repository: SoleRepository, private val context: Context) : ViewModel() {
    private val geminiService = GeminiService(context)
    val messagingRepository = MessagingRepository(context)

    // Creator & User account state
    private val _userAccount = MutableStateFlow<UserAccount?>(null)
    val userAccount: StateFlow<UserAccount?> = _userAccount.asStateFlow()

    private val _isOnboarded = MutableStateFlow(false)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _isCreatorMode = MutableStateFlow(false)
    val isCreatorMode = _isCreatorMode.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }

    init {
        loadAccountFromPrefs(context)
        viewModelScope.launch {
            messagingRepository.prepopulateDefaultConversations()
        }
    }

    val creators: StateFlow<List<Creator>> = repository.allCreators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<Post>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallet: StateFlow<UserWallet?> = repository.userWallet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val creatorAlbums: StateFlow<List<CreatorAlbum>> = repository.getAlbumsByCreator("local_creator")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creatorMedia: StateFlow<List<CreatorMediaItem>> = repository.getAllMediaByCreator("local_creator")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = messagingRepository.getConversationsForUser("local_user")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<Message>> = _activeConversationId
        .flatMapLatest { id ->
            if (id != null) messagingRepository.streamMessages(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            messagingRepository.markAsRead(conversationId)
        }
    }

    fun sendMessageToActive(text: String, mediaUri: String? = null, isPremium: Boolean = false, unlockPriceCoins: Double = 0.0) {
        val conversationId = _activeConversationId.value ?: return
        viewModelScope.launch {
            val user = _userAccount.value
            val message = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "local_user",
                senderName = user?.username ?: "Local Fan",
                senderAvatar = "img_purple_bg_1782877584581",
                text = text,
                timestamp = System.currentTimeMillis(),
                mediaUri = mediaUri,
                isPremium = isPremium,
                unlockPriceCoins = unlockPriceCoins,
                isUnlocked = !isPremium,
                status = "SENT"
            )
            messagingRepository.sendMessage(message)
        }
    }

    fun simulateReceiveMessageInActive(senderId: String, text: String, mediaUri: String? = null, isPremium: Boolean = false, unlockPriceCoins: Double = 0.0) {
        val conversationId = _activeConversationId.value ?: return
        viewModelScope.launch {
            val creatorsList = creators.value
            val creator = creatorsList.find { it.id == senderId }
            val senderName = creator?.name ?: "Creator"
            val senderAvatar = creator?.avatarRes ?: "img_open_toes_pink_1782962118574"
            val message = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = text,
                timestamp = System.currentTimeMillis(),
                mediaUri = mediaUri,
                isPremium = isPremium,
                unlockPriceCoins = unlockPriceCoins,
                isUnlocked = false,
                status = "DELIVERED"
            )
            messagingRepository.sendMessage(message)
        }
    }

    fun setConversationHeldState(conversationId: String, isHeld: Boolean) {
        viewModelScope.launch {
            messagingRepository.setConversationHeld(conversationId, isHeld)
            _userMessage.value = if (isHeld) "Spam Filter: Conversation placed on Hold." else "Spam Filter: Released from Hold."
        }
    }

    fun createNewDirectMessage(creatorId: String, title: String) {
        viewModelScope.launch {
            val conversationId = "conv_$creatorId"
            val existing = messagingRepository.getConversationById(conversationId)
            if (existing == null) {
                val newConv = Conversation(
                    id = conversationId,
                    title = title,
                    lastMessage = "Started a new conversation with $title",
                    unreadCount = 0,
                    timestamp = System.currentTimeMillis(),
                    isLocked = false,
                    isHeld = false,
                    tier = "PRIMARY"
                )
                messagingRepository.insertConversationDirect(newConv)
                messagingRepository.insertParticipantDirect(Participant("${conversationId}_local", conversationId, "local_user", "FAN"))
                messagingRepository.insertParticipantDirect(Participant("${conversationId}_creator", conversationId, creatorId, "CREATOR"))
            }
            selectConversation(conversationId)
            _currentRoute.value = "messages"
        }
    }

    fun unlockPremiumMessage(message: Message) {
        viewModelScope.launch {
            val currentWallet = repository.getDirectWalletForUser("local_user") ?: UserWallet()
            if (currentWallet.balance >= message.unlockPriceCoins) {
                val newBalance = currentWallet.balance - message.unlockPriceCoins
                repository.updateWalletBalanceDirect("local_user", newBalance)
                messagingRepository.updateMessage(message.copy(isUnlocked = true))
                _userMessage.value = "Successfully unlocked premium content for ${message.unlockPriceCoins} Gold Coins!"
            } else {
                _userMessage.value = "Insufficient coins! Refuel your wallet in the Ledger tab."
            }
        }
    }


    // Current navigation state
    private val _currentRoute = MutableStateFlow("feed")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedCreatorId = MutableStateFlow<String?>(null)
    val selectedCreatorId: StateFlow<String?> = _selectedCreatorId.asStateFlow()

    private val _selectedPostId = MutableStateFlow<String?>(null)
    val selectedPostId: StateFlow<String?> = _selectedPostId.asStateFlow()

    // Screen Loading/Shimmering states for smooth skeleton transitions
    private val _isExploreLoading = MutableStateFlow(false)
    val isExploreLoading = _isExploreLoading.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading = _isFeedLoading.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading = _isProfileLoading.asStateFlow()

    fun simulateExploreLoad() {
        viewModelScope.launch {
            _isExploreLoading.value = true
            kotlinx.coroutines.delay(1000)
            _isExploreLoading.value = false
        }
    }

    fun simulateFeedLoad() {
        viewModelScope.launch {
            _isFeedLoading.value = true
            kotlinx.coroutines.delay(1200)
            _isFeedLoading.value = false
        }
    }

    fun simulateProfileLoad() {
        viewModelScope.launch {
            _isProfileLoading.value = true
            kotlinx.coroutines.delay(1000)
            _isProfileLoading.value = false
        }
    }

    // Active Comments
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeComments: Flow<List<Comment>> = _selectedPostId
        .flatMapLatest { postId ->
            if (postId != null) repository.getCommentsForPost(postId) else flowOf(emptyList())
        }

    // AI Analysis states
    private val _aiState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiState: StateFlow<AiAnalysisState> = _aiState.asStateFlow()

    // Message system for interactive user tips, unlocks, deposits
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // --- 5 VIRAL FEATURES STATES & BUSINESS LOGIC ---

    // Feature 5: Fan Footprint Loyalty Points
    private val _footprintPoints = MutableStateFlow(160) // Starts at Silver Sole
    val footprintPoints = _footprintPoints.asStateFlow()

    fun earnFootprintPoints(amount: Int, reason: String) {
        _footprintPoints.value += amount
        _userMessage.value = "Loyalty Perk: Unlocked +$amount Footprint Points! ($reason) 👟🏆"
    }

    // Feature 1: Sole Match AI Preferator and Preferences Match UI state
    val matchNail = MutableStateFlow("Pastel Pink")
    val matchArch = MutableStateFlow("High Arch")
    val matchShoe = MutableStateFlow("Designer Stilettos")
    val matchSkin = MutableStateFlow("Warm Bronze")

    // Dynamic swipe list generated locally for Match Profiles
    val rawMatchProfiles = listOf(
        MatchProfile(
            id = "match_anastasia",
            name = "Anastasia Rose",
            handle = "@anarose_heels",
            imageResName = "img_open_toes_pink_1782962118574",
            archType = "High Arch",
            nailStyle = "Glossy Hot Pink",
            shoeType = "Open-Toe Sandals",
            skinTone = "Porcelain White",
            bio = "I adore styling premium open-toe luxury sandals and elegant sexy slippers with bright polished nails."
        ),
        MatchProfile(
            id = "match_sasha",
            name = "Sasha Blue",
            handle = "@sashapedicure",
            imageResName = "img_model2_pedicure_1781222811962",
            archType = "Medium Flat",
            nailStyle = "Pastel Pink",
            shoeType = "Barefoot Soft",
            skinTone = "Fair Beige",
            bio = "Pedicure artist specializing in cushion aesthetics and relaxing designs."
        ),
        MatchProfile(
            id = "match_elena",
            name = "Elena Wilde",
            handle = "@elenaparty",
            imageResName = "img_model3_anklet_1781222826340",
            archType = "Elegant Curve",
            nailStyle = "Bare Natural",
            shoeType = "Beach Sandals",
            skinTone = "Warm Bronze",
            bio = "Cozy pool slides, hand-crafted gold chains, summer aesthetic specialist."
        ),
        MatchProfile(
            id = "match_clara",
            name = "Clara Lace",
            handle = "@claralace",
            imageResName = "img_sexy_slippers_yellow_1782962148817",
            archType = "High Arch",
            nailStyle = "Vibrant Neon Yellow",
            shoeType = "Sexy Boudoir Slippers",
            skinTone = "Sun-Kissed Golden",
            bio = "Elegant open-back slides, sexy feather-trimmed slippers, and bright neon polished nails in cozy high-fashion settings."
        )
    )

    // Feature 2: Live Sole Session - Queue, Leaderboard, Explosions
    private val _liveTippers = MutableStateFlow(listOf(
        "SoleLover99" to 550.0,
        "ArchEnthusiast" to 425.0,
        "HeelMaster" to 220.0,
        "You" to 0.0
    ))
    val liveTippers = _liveTippers.asStateFlow()

    private val _liveRequests = MutableStateFlow(listOf(
        LiveRequest("req_1", "High heel runway pivot posing 👑", 40.0, 18),
        LiveRequest("req_2", "Detailed pedicure close-up zoom 💅", 50.0, 22),
        LiveRequest("req_3", "Anklet dynamic sunset walk 🌟", 65.0, 15)
    ))
    val liveRequests = _liveRequests.asStateFlow()

    // Counter triggers animated floating tip explosions in UI
    private val _tipExplosionTrigger = MutableStateFlow(0)
    val tipExplosionTrigger = _tipExplosionTrigger.asStateFlow()

    // Feature 2 Extended States
    private val _floatingTips = MutableStateFlow<List<FloatingTip>>(emptyList())
    val floatingTips = _floatingTips.asStateFlow()

    private val _liveViewerCount = MutableStateFlow(1248)
    val liveViewerCount = _liveViewerCount.asStateFlow()

    private val _sessionEarnings = MutableStateFlow(1195.0)
    val sessionEarnings = _sessionEarnings.asStateFlow()

    private val _celebrationText = MutableStateFlow<String?>(null)
    val celebrationText = _celebrationText.asStateFlow()

    private val _activePoseId = MutableStateFlow<String?>("req_1")
    val activePoseId = _activePoseId.asStateFlow()

    fun triggerTipExplosion(emojiList: List<String> = listOf("💖", "🪙", "👠", "💅", "🦶", "✨")) {
        _tipExplosionTrigger.value += 1
        val currentList = _floatingTips.value.toMutableList()
        val random = java.util.Random()
        emojiList.forEachIndexed { index, emoji ->
            val id = "tip_${System.currentTimeMillis()}_${random.nextInt(100000)}_${index}"
            currentList.add(
                FloatingTip(
                    id = id,
                    emoji = emoji,
                    label = if (emoji == "🪙") "+$" else "",
                    colorHex = if (emoji == "💖") 0xFFEC4899.toInt() else if (emoji == "🪙") 0xFFFFB800.toInt() else 0xFFFFFFFF.toInt(),
                    xOffset = 0.1f + random.nextFloat() * 0.8f,
                    delayMs = index * 100L
                )
            )
        }
        _floatingTips.value = currentList.takeLast(30)
    }

    fun removeFloatingTip(id: String) {
        _floatingTips.value = _floatingTips.value.filter { it.id != id }
    }

    fun toggleCreatorMode(isCreator: Boolean) {
        _isCreatorMode.value = isCreator
    }

    fun pinRequestAsActive(requestId: String?) {
        _activePoseId.value = requestId
    }

    fun dismissRequest(requestId: String) {
        _liveRequests.value = _liveRequests.value.filter { it.id != requestId }
    }

    fun selectCelebration(text: String?) {
        _celebrationText.value = text
    }

    fun clearCelebration() {
        _celebrationText.value = null
    }

    fun createLiveRequest(description: String, cost: Double) {
        val id = "req_${System.currentTimeMillis()}"
        val newReq = LiveRequest(id = id, description = description, coinCost = cost, votesCount = 0)
        _liveRequests.value = (_liveRequests.value + newReq).sortedByDescending { it.votesCount }
        _userMessage.value = "New active fan request goal added!"
    }

    fun completeRequest(requestId: String) {
        val req = _liveRequests.value.find { it.id == requestId } ?: return
        _liveRequests.value = _liveRequests.value.filter { it.id != requestId }
        if (_activePoseId.value == requestId) {
            _activePoseId.value = _liveRequests.value.firstOrNull()?.id
        }
        _celebrationText.value = "Anastasia Rose completed: ${req.description}! 🎉"
        earnFootprintPoints(75, "Completed live event request")
        _sessionEarnings.value += req.coinCost
    }

    // Feature 3: Step Challenges with live progress voting
    private val _challenges = MutableStateFlow(listOf(
        StepChallenge(
            id = "chall_1",
            title = "Aesthetic Summer Sandals 🏖️",
            subtitle = "Sasha's pastel beach sandals vs Elena's sun-kissed pool slides. Vote for your absolute favorite!",
            candidateAName = "Sasha Blue",
            candidateBName = "Elena Wilde",
            candidateAAvatar = "img_model2_pedicure_1781222811962",
            candidateBAvatar = "img_model3_anklet_1781222826340",
            candidateAImage = "img_model2_pedicure_1781222811962",
            candidateBImage = "img_model3_anklet_1781222826340",
            candidateAVotes = 345,
            candidateBVotes = 412,
            userVotedFor = null
        ),
        StepChallenge(
            id = "chall_2",
            title = "The Sensual Open Toe Contest 👡",
            subtitle = "Anastasia's hot pink open-toe luxury sandals vs Clara's yellow feather-trimmed sexy slippers. Bright nails show down!",
            candidateAName = "Anastasia Rose",
            candidateBName = "Clara Lace",
            candidateAAvatar = "img_open_toes_pink_1782962118574",
            candidateBAvatar = "img_sexy_slippers_yellow_1782962148817",
            candidateAImage = "img_open_toes_pink_1782962118574",
            candidateBImage = "img_sexy_slippers_yellow_1782962148817",
            candidateAVotes = 582,
            candidateBVotes = 560,
            userVotedFor = null
        )
    ))
    val challenges = _challenges.asStateFlow()

    fun voteInChallenge(challengeId: String, voteCandidate: String) {
        _challenges.value = _challenges.value.map { challenge ->
            if (challenge.id == challengeId && challenge.userVotedFor == null) {
                earnFootprintPoints(25, "Step Challenge Voting Activity")
                if (voteCandidate == "A") {
                    challenge.copy(
                        candidateAVotes = challenge.candidateAVotes + 1,
                        userVotedFor = "A"
                    )
                } else {
                    challenge.copy(
                        candidateBVotes = challenge.candidateBVotes + 1,
                        userVotedFor = "B"
                    )
                }
            } else {
                challenge
            }
        }
    }

    // Feature 4: Limited Signed NFT Prints
    private val _ownedNfts = MutableStateFlow<Set<String>>(emptySet())
    val ownedNfts = _ownedNfts.asStateFlow()

    data class DigitalNftPrint(
        val id: String,
        val title: String,
        val creatorName: String,
        val cost: Double,
        val imageResource: String,
        val rarity: String, // "LEGENDARY", "EPIC", "RARE"
        val remainingCount: Int
    )

    val nftCatalog = listOf(
        DigitalNftPrint("nft_1", "Glossy Pink Open-Toe Sandal Portrait #03", "Anastasia Rose", 60.0, "img_open_toes_pink_1782962118574", "LEGENDARY", 2),
        DigitalNftPrint("nft_2", "Pastel Silk Pedicure Fine Print #09", "Sasha Blue", 45.0, "img_model2_pedicure_1781222811962", "RARE", 8),
        DigitalNftPrint("nft_3", "Gold Sandal Sunset Portrait #01", "Elena Wilde", 55.0, "img_model3_anklet_1781222826340", "EPIC", 4),
        DigitalNftPrint("nft_4", "Sexy Yellow Feather Slippers Masterpiece #02", "Clara Lace", 75.0, "img_sexy_slippers_yellow_1782962148817", "LEGENDARY", 1)
    )

    fun purchaseNft(nftId: String) {
        val nft = nftCatalog.find { it.id == nftId } ?: return
        if (_ownedNfts.value.contains(nftId)) {
            _userMessage.value = "You already own this limited signed print in your trophy case!"
            return
        }

        viewModelScope.launch {
            val walletDirect = wallet.value ?: return@launch
            if (walletDirect.balance >= nft.cost) {
                // Deduct balance manually
                repository.sendTipToCreator("creator_anastasia", nft.cost) // Use sendTip logic to transfer funds
                _ownedNfts.value = _ownedNfts.value + nftId
                earnFootprintPoints(50, "Signed Digital NFT Print Purchase")
                _userMessage.value = "NFT Minted! Check your Trophy Case for the Signed ${nft.title}! 🏆🎨"
            } else {
                _userMessage.value = "Insufficient gold balance! Tap refuel above to earn free coins."
            }
        }
    }

    // Interactive tipping inside Live Stream Session
    fun sendLiveTip(amount: Double) {
        viewModelScope.launch {
            val walletDirect = wallet.value ?: return@launch
            if (walletDirect.balance >= amount) {
                repository.sendTipToCreator("creator_anastasia", amount) // TipAnastasia for live
                
                // Add to tipper list
                _liveTippers.value = _liveTippers.value.map { pair ->
                    if (pair.first == "You") "You" to (pair.second + amount) else pair
                }.sortedByDescending { it.second }

                earnFootprintPoints((amount * 1.5).toInt(), "Live Session Interactive Tipping")
                triggerTipExplosion()
                _userMessage.value = "Sent live $$amount Tip! 🔥 BOOM! Interactive Tip Explosion Activated!"
            } else {
                _userMessage.value = "Insufficient coins! Tap Refuel to support this live-stream."
            }
        }
    }

    // Add Interactive Live Director Request voting/bumping
    fun voteOrAddLiveRequest(requestId: String) {
        val req = _liveRequests.value.find { it.id == requestId } ?: return
        viewModelScope.launch {
            val walletDirect = wallet.value ?: return@launch
            if (walletDirect.balance >= req.coinCost) {
                repository.sendTipToCreator("creator_anastasia", req.coinCost)
                _liveRequests.value = _liveRequests.value.map { item ->
                    if (item.id == requestId) item.copy(votesCount = item.votesCount + 1) else item
                }.sortedByDescending { it.votesCount }
                earnFootprintPoints((req.coinCost * 1.2).toInt(), "Director Request Contribution")
                triggerTipExplosion()
                _userMessage.value = "Contributed to target! Bumped '${req.description}' in the Request Queue."
            } else {
                _userMessage.value = "Insufficient balance! Please refuel your wallet."
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
        // Initial simulated fetch to show off skeletal shimmers
        simulateFeedLoad()
        simulateExploreLoad()
        // Real-time simulated active audience loop
        viewModelScope.launch {
            val names = listOf("PedicurePrince", "StilettoFanatic", "VelvetAnkle", "ArchQueen", "AuraLover", "StepCrazer", "SoleSensation")
            val emojis = listOf("💖", "🪙", "👠", "💅", "🦶", "✨")
            val random = java.util.Random()
            // Wait slightly before booting simulated activity to avoid competing with database prepopulation
            kotlinx.coroutines.delay(5000)
            while (true) {
                kotlinx.coroutines.delay(12000) // tick every 12 seconds
                
                // fluctuate viewers slightly
                val delta = random.nextInt(26) - 10
                _liveViewerCount.value = (_liveViewerCount.value + delta).coerceIn(400, 3500)

                // occasionally simulate a tip or custom request vote
                val roll = random.nextFloat()
                if (roll < 0.35f) {
                    val name = names[random.nextInt(names.size)]
                    val amt = listOf(10.0, 25.0, 50.0)[random.nextInt(3)]
                    
                    // Increment session earnings
                    _sessionEarnings.value += amt

                    // Update live tippers list securely
                    val currentTippers = _liveTippers.value
                    val existingIndex = currentTippers.indexOfFirst { it.first == name }
                    if (existingIndex >= 0) {
                        _liveTippers.value = currentTippers.mapIndexed { i, pair ->
                            if (i == existingIndex) pair.first to (pair.second + amt) else pair
                        }.sortedByDescending { it.second }
                    } else {
                        val updated = currentTippers.toMutableList()
                        updated.add(name to amt)
                        _liveTippers.value = updated.sortedByDescending { it.second }
                    }

                    // float icons
                    triggerTipExplosion(listOf(emojis[random.nextInt(emojis.size)], "🪙", emojis[random.nextInt(emojis.size)]))
                    _userMessage.value = "Audience: @$name tipped $amt Coins!"
                } else if (roll < 0.70f) {
                    // Simulated vote/bump on requests
                    val currentRequests = _liveRequests.value
                    if (currentRequests.isNotEmpty()) {
                        val reqToBump = currentRequests[random.nextInt(currentRequests.size)]
                        _liveRequests.value = currentRequests.map { item ->
                            if (item.id == reqToBump.id) item.copy(votesCount = item.votesCount + 1) else item
                        }.sortedByDescending { it.votesCount }
                        
                        triggerTipExplosion(listOf("✨", "💖"))
                    }
                }
            }
        }
    }

    private var lastNavigationTime = 0L

    fun navigateTo(route: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < 650L) return
        lastNavigationTime = currentTime
        _currentRoute.value = route
        if (route == "explore") {
            simulateExploreLoad()
        } else if (route == "feed") {
            simulateFeedLoad()
        }
    }

    fun selectCreator(creatorId: String?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < 600L) return
        lastNavigationTime = currentTime
        _selectedCreatorId.value = creatorId
        if (creatorId != null) {
            _currentRoute.value = "creator_detail"
            simulateProfileLoad()
        }
    }

    fun selectPost(postId: String?) {
        _selectedPostId.value = postId
        if (postId != null) {
            _currentRoute.value = "post_detail"
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(postId)
            earnFootprintPoints(5, "Liked a designer portfolio entry")
        }
    }

    fun postComment(postId: String, text: String) {
        viewModelScope.launch {
            repository.addComment(postId, text)
            earnFootprintPoints(10, "Added fashion critique feedback")
        }
    }

    fun executeSubscriptionPurchase(fanId: String, creatorId: String, cost: Double) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val success = repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, cost)
                if (success) {
                    _paymentState.value = PaymentState.Success
                    earnFootprintPoints(40, "Premium Model Access Subscription")
                    _userMessage.value = "Subscription successful! 80/20 platform split processed."
                    
                    // If the logged in user has role CREATOR and matches creatorId (or for local simulation purposes),
                    // sync the in-memory/SharedPrefs account balance as well.
                    val currentAccount = _userAccount.value
                    if (currentAccount != null && currentAccount.role == "CREATOR") {
                        val creatorShare = cost * 0.80
                        addCreatorEarnings(creatorShare, context)
                    }
                } else {
                    _paymentState.value = PaymentState.Error("Insufficient Funds")
                    _userMessage.value = "Insufficient balance! Tap refuel above to add simulated coins."
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Transaction Error")
                _userMessage.value = "Transaction failed: ${e.message}"
            }
        }
    }

    fun subscribe(creatorId: String) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val creatorsList = creators.value
                val creator = creatorsList.find { it.id == creatorId }
                if (creator == null) {
                    _paymentState.value = PaymentState.Error("Creator not found")
                    return@launch
                }

                if (creator.subScriptionActive) {
                    // Already subscribed, unsubscribe is free
                    val success = repository.subscribeToCreator(creatorId)
                    if (success) {
                        _paymentState.value = PaymentState.Success
                        _userMessage.value = "Unsubscribed successfully."
                    } else {
                        _paymentState.value = PaymentState.Error("Failed to unsubscribe")
                    }
                } else {
                    // Subscribe via the 80/20 transaction split engine
                    val success = repository.executeSubscriptionPurchaseInRepo("local_user", creatorId, creator.subPrice)
                    if (success) {
                        _paymentState.value = PaymentState.Success
                        earnFootprintPoints(40, "Premium Model Access Subscription")
                        _userMessage.value = "Subscription successful! 80/20 platform split processed."
                        
                        val currentAccount = _userAccount.value
                        if (currentAccount != null && currentAccount.role == "CREATOR") {
                            val creatorShare = creator.subPrice * 0.80
                            addCreatorEarnings(creatorShare, context)
                        }
                    } else {
                        _paymentState.value = PaymentState.Error("Insufficient Funds")
                        _userMessage.value = "Insufficient balance! Tap refuel above to add simulated coins."
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Subscription Error")
                _userMessage.value = "Subscription transaction failed."
            }
        }
    }

    fun activateCryptoSubscription(creatorId: String) {
        viewModelScope.launch {
            val success = repository.activateSubscriptionViaCrypto(creatorId)
            if (success) {
                earnFootprintPoints(100, "Crypto USDT Premium Subscription")
                _userMessage.value = "Payment confirmed! VIP Access Unlocked ✨"
            } else {
                _userMessage.value = "Activation failed. Please contact support."
            }
        }
    }

    fun unlockPost(postId: String) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val success = repository.unlockPremiumPost(postId)
                if (success) {
                    _paymentState.value = PaymentState.Success
                    earnFootprintPoints(20, "Unlocked premium visual portfolio entry")
                    _userMessage.value = "Premium post visual unlocked! Enjoy!"
                    
                    val currentAccount = _userAccount.value
                    val p = posts.value.find { it.id == postId }
                    if (p != null && currentAccount != null && currentAccount.role == "CREATOR") {
                        val creatorShare = p.unlockPrice * 0.80
                        addCreatorEarnings(creatorShare, context)
                    }
                } else {
                    _paymentState.value = PaymentState.Error("Insufficient Funds")
                    _userMessage.value = "Insufficient balance! Tap refuel above to earn free coins."
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Unlock Error")
                _userMessage.value = "Unlock transaction failed."
            }
        }
    }

    fun unlockCreatorAlbum(albumId: String) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val success = repository.unlockCreatorAlbum(albumId)
                if (success) {
                    _paymentState.value = PaymentState.Success
                    earnFootprintPoints(30, "Unlocked premium visual folder")
                    _userMessage.value = "Premium folder unlocked! Enjoy all contents!"
                } else {
                    _paymentState.value = PaymentState.Error("Insufficient Funds")
                    _userMessage.value = "Insufficient balance! Tap refuel above to earn free coins."
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Unlock Error")
                _userMessage.value = "Folder unlock transaction failed."
            }
        }
    }

    fun unlockCreatorMediaItem(mediaId: String) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val success = repository.unlockCreatorMediaItem(mediaId)
                if (success) {
                    _paymentState.value = PaymentState.Success
                    earnFootprintPoints(15, "Unlocked premium portfolio item")
                    _userMessage.value = "Content item unlocked! Enjoy!"
                } else {
                    _paymentState.value = PaymentState.Error("Insufficient Funds")
                    _userMessage.value = "Insufficient balance! Tap refuel above to earn free coins."
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Unlock Error")
                _userMessage.value = "Content unlock transaction failed."
            }
        }
    }

    fun sendTip(creatorId: String, amount: Double) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            try {
                val success = repository.sendTipToCreator(creatorId, amount)
                if (success) {
                    _paymentState.value = PaymentState.Success
                    earnFootprintPoints((amount * 2.0).toInt(), "Direct creator tip support")
                    _userMessage.value = "You sent a $$amount tip! The model is highly appreciative!"
                    
                    val currentAccount = _userAccount.value
                    if (currentAccount != null && currentAccount.role == "CREATOR") {
                        val creatorShare = amount * 0.80
                        addCreatorEarnings(creatorShare, context)
                    }
                } else {
                    _paymentState.value = PaymentState.Error("Insufficient Funds")
                    _userMessage.value = "Insufficient balance! Refuel your wallet to support models."
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Tip Error")
                _userMessage.value = "Tip transaction failed."
            }
        }
    }

    fun refillWallet() {
        viewModelScope.launch {
            repository.addSimulatedCoins(100.0)
            _userMessage.value = "Simulated Wallet Refuel: Added $100.00 Gold! 💸"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun triggerAiStyleCheck(bitmap: Bitmap?, contextString: String) {
        _aiState.value = AiAnalysisState.Loading
        viewModelScope.launch {
            try {
                val markdownResult = geminiService.analyzeFootwearAndLegwear(bitmap, contextString)
                _aiState.value = AiAnalysisState.Success(markdownResult)
            } catch (e: Exception) {
                _aiState.value = AiAnalysisState.Error(e.localizedMessage ?: "Unknown analysis error")
            }
        }
    }

    fun resetAiState() {
        _aiState.value = AiAnalysisState.Idle
    }

    fun uploadSimulatedPost(
        creatorId: String,
        caption: String,
        imageName: String,
        postType: String,
        price: Double
    ) {
        viewModelScope.launch {
            val creator = creators.value.find { it.id == creatorId } ?: return@launch
            repository.uploadCustomPost(
                creatorId = creatorId,
                creatorName = creator.name,
                creatorHandle = creator.handle,
                avatarRes = creator.avatarRes,
                caption = caption,
                imageName = imageName,
                postType = postType,
                unlockPrice = price
            )
            _userMessage.value = "Simulated Model Post created successfully! Verified in feed."
        }
    }

    // --- ACCREDITED CREATOR & FAN INFRASTRUCTURE ---

    fun loadAccountFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
        val onboarded = prefs.getBoolean("onboarded", false)
        if (onboarded) {
            val username = prefs.getString("username", "Guest") ?: "Guest"
            val handle = prefs.getString("handle", "@guest") ?: "@guest"
            val role = prefs.getString("role", "USER") ?: "USER"
            val subPrice = prefs.getFloat("sub_price", 9.99f).toDouble()
            val unlockFee = prefs.getFloat("unlock_fee", 4.99f).toDouble()
            val balance = prefs.getFloat("balance", 125.0f).toDouble()
            val bio = prefs.getString("bio", "Elite fashion enthusiast.") ?: "Elite fashion enthusiast."
            val avatarRes = prefs.getString("avatar_res", "img_app_icon_1781222852965") ?: "img_app_icon_1781222852965"
            val bannerRes = prefs.getString("banner_res", "img_beach_shower_1783035001001") ?: "img_beach_shower_1783035001001"
            val backgroundRes = prefs.getString("background_res", "luxury_trio_bg_v2_1783031169841") ?: "luxury_trio_bg_v2_1783031169841"
            _userAccount.value = UserAccount(
                username = username,
                handle = handle,
                role = role,
                subPrice = subPrice,
                unlockFee = unlockFee,
                balance = balance,
                avatarRes = avatarRes,
                bio = bio,
                bannerRes = bannerRes,
                backgroundRes = backgroundRes
            )
            _isOnboarded.value = true
            _isCreatorMode.value = (role == "CREATOR")
        } else {
            _userAccount.value = null
            _isOnboarded.value = false
        }
    }

    fun registerAccount(username: String, handle: String, role: String, bio: String, context: Context) {
        val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
        val current = _userAccount.value
        val subPrice = current?.subPrice ?: prefs.getFloat("sub_price", 9.99f).toDouble()
        val unlockFee = current?.unlockFee ?: prefs.getFloat("unlock_fee", 4.99f).toDouble()
        val balance = current?.balance ?: prefs.getFloat("balance", 125.0f).toDouble()
        val avatarRes = current?.avatarRes ?: "img_app_icon_1781222852965"
        val bannerRes = current?.bannerRes ?: "img_beach_shower_1783035001001"
        val backgroundRes = current?.backgroundRes ?: "luxury_trio_bg_v2_1783031169841"

        prefs.edit()
            .putBoolean("onboarded", true)
            .putString("username", username)
            .putString("handle", handle)
            .putString("role", role)
            .putString("bio", bio)
            .putFloat("sub_price", subPrice.toFloat())
            .putFloat("unlock_fee", unlockFee.toFloat())
            .putFloat("balance", balance.toFloat())
            .putString("avatar_res", avatarRes)
            .putString("banner_res", bannerRes)
            .putString("background_res", backgroundRes)
            .apply()

        _userAccount.value = UserAccount(
            username = username,
            handle = handle,
            role = role,
            subPrice = subPrice,
            unlockFee = unlockFee,
            balance = balance,
            avatarRes = avatarRes,
            bio = bio,
            bannerRes = bannerRes,
            backgroundRes = backgroundRes
        )
        _isOnboarded.value = true
        _isCreatorMode.value = (role == "CREATOR")
        _userMessage.value = "Welcome to SoleLuxe, $username! Role: $role registered."
    }

    fun updateCreatorPricing(subPrice: Double, unlockFee: Double, context: Context) {
        if (subPrice < 0.0 || unlockFee < 0.0) {
            throw IllegalArgumentException("Pricing values cannot be negative")
        }
        val current = _userAccount.value ?: return
        val updated = current.copy(subPrice = subPrice, unlockFee = unlockFee)
        _userAccount.value = updated
        val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("sub_price", subPrice.toFloat())
            .putFloat("unlock_fee", unlockFee.toFloat())
            .apply()
        _userMessage.value = "Creator pricing updated successfully!"
    }

    fun addCreatorEarnings(amount: Double, context: Context) {
        val current = _userAccount.value ?: return
        val updated = current.copy(balance = current.balance + amount)
        _userAccount.value = updated
        val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("balance", updated.balance.toFloat())
            .apply()
    }

    fun logoutAccount(context: Context) {
        val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _userAccount.value = null
        _isOnboarded.value = false
        _currentRoute.value = "feed"
        _userMessage.value = "Logged out successfully."
    }

    // --- Creator Portfolio Management Methods ---

    fun createCreatorAlbum(name: String, description: String, price: Double, coverRes: String) {
        viewModelScope.launch {
            val album = CreatorAlbum(
                id = UUID.randomUUID().toString(),
                creatorId = "local_creator",
                name = name,
                description = description,
                price = price,
                coverRes = coverRes
            )
            repository.insertAlbum(album)
            _userMessage.value = "Album '$name' created successfully!"
        }
    }

    fun addCreatorMediaItem(albumId: String?, title: String, mediaUri: String, mediaType: String, price: Double) {
        viewModelScope.launch {
            val item = CreatorMediaItem(
                id = UUID.randomUUID().toString(),
                creatorId = "local_creator",
                albumId = albumId,
                title = title,
                mediaUri = mediaUri,
                mediaType = mediaType,
                price = price
            )
            repository.insertMediaItem(item)
            _userMessage.value = "Content '$title' uploaded to portfolio!"
        }
    }

    fun addStrategicCreatorMediaItem(
        albumId: String?, 
        title: String, 
        mediaUri: String, 
        mediaType: String, 
        price: Double,
        collaboratorId: String? = null,
        collaboratorSplit: Double = 0.0,
        isPreOrder: Boolean = false,
        releaseTimestamp: Long = 0L,
        arOverlayType: String? = null,
        arOverlayIntensity: Float = 1.0f
    ) {
        viewModelScope.launch {
            val item = CreatorMediaItem(
                id = java.util.UUID.randomUUID().toString(),
                creatorId = "local_creator",
                albumId = albumId,
                title = title,
                mediaUri = mediaUri,
                mediaType = mediaType,
                price = price,
                collaboratorId = collaboratorId,
                collaboratorSplit = collaboratorSplit,
                isPreOrder = isPreOrder,
                releaseTimestamp = releaseTimestamp,
                isPrePurchased = false,
                arOverlayType = arOverlayType,
                arOverlayIntensity = arOverlayIntensity
            )
            repository.insertMediaItem(item)
            _userMessage.value = "Content '$title' custom-rendered with Strategic Editorial FX!"
        }
    }

    fun deleteCreatorAlbum(id: String) {
        viewModelScope.launch {
            repository.deleteAlbum(id)
            _userMessage.value = "Album and all its contents deleted."
        }
    }

    fun deleteCreatorMediaItem(id: String) {
        viewModelScope.launch {
            repository.deleteMediaItem(id)
            _userMessage.value = "Media item deleted successfully."
        }
    }

    fun updateCreatorProfile(name: String, bio: String, subPrice: Double, avatarRes: String, bannerRes: String, backgroundRes: String, context: Context) {
        viewModelScope.launch {
            val current = _userAccount.value
            if (current != null) {
                val updated = current.copy(
                    username = name,
                    bio = bio,
                    subPrice = subPrice,
                    avatarRes = avatarRes,
                    bannerRes = bannerRes,
                    backgroundRes = backgroundRes
                )
                _userAccount.value = updated
                
                val prefs = context.getSharedPreferences("soleluxe_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("username", name)
                    .putString("bio", bio)
                    .putFloat("sub_price", subPrice.toFloat())
                    .putString("avatar_res", avatarRes)
                    .putString("banner_res", bannerRes)
                    .putString("background_res", backgroundRes)
                    .apply()

                // Also upsert in database creators table
                val dbCreator = Creator(
                    id = "local_creator",
                    name = name,
                    handle = current.handle,
                    avatarRes = avatarRes,
                    bannerRes = bannerRes,
                    bio = bio,
                    subPrice = subPrice,
                    subScriptionActive = false,
                    categoryTags = "Aesthetic,Portfolio",
                    popularityScore = 80,
                    verified = true,
                    cryptoWalletAddress = "0xCreatorWallet..."
                )
                // Insert or update
                val existing = creators.value.find { it.id == "local_creator" }
                if (existing != null) {
                    repository.updateCreator(dbCreator)
                } else {
                    repository.insertCreators(listOf(dbCreator))
                }
                _userMessage.value = "Profile customized successfully!"
            }
        }
    }
}

class MainViewModelFactory(private val repository: SoleRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
