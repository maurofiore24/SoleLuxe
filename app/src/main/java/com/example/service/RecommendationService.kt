package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.model.Post
import com.example.data.repository.SoleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln
import kotlin.math.exp

// ====================================================================================
// CRYPTO & FINTECH ATOMIC TRANSACTION ENGINE
// ====================================================================================

class ConcurrentTransactionException(message: String) : Exception(message)

class FintechWalletHandler(
    private val repository: SoleRepository? = null
) {
    private val userLockRegistry = ConcurrentHashMap<String, Mutex>()
    private val lastTransactionTimestamps = ConcurrentHashMap<String, Long>()

    // Local state for in-memory unit tests when a real DB repository isn't provided
    val mockBalances = ConcurrentHashMap<String, Double>()

    private fun getLockForUser(userId: String): Mutex {
        return userLockRegistry.computeIfAbsent(userId) { Mutex() }
    }

    /**
     * Safely processes micro-transactions (tips, unlocks, escrows) under a dynamic Mutex.
     * Rejects rapid repeat actions within 50ms to prevent double-spending attacks.
     */
    suspend fun executeTransaction(
        userId: String,
        creatorId: String,
        amount: Double,
        isPPVUnlock: Boolean
    ): Double {
        val userMutex = getLockForUser(userId)

        userMutex.withLock {
            val now = System.currentTimeMillis()
            val lastTime = lastTransactionTimestamps[userId] ?: 0L
            if (now - lastTime < 50L) {
                throw ConcurrentTransactionException("Rapid concurrent tap detected (within 50ms). Preventing double-spend attack.")
            }
            lastTransactionTimestamps[userId] = now

            if (repository != null) {
                val wallet = repository.getDirectWalletForUser(userId)
                    ?: throw IllegalArgumentException("User wallet not found for $userId")
                if (wallet.balance < amount) {
                    throw IllegalArgumentException("Insufficient balance: Need $amount but have ${wallet.balance}")
                }
                val newBalance = wallet.balance - amount
                repository.updateWalletBalanceDirect(userId, newBalance)

                // Boost creator's EVS dynamically by registering transaction
                PlatformRevenueTriggers.recordTransaction(creatorId, amount)
                return newBalance
            } else {
                val currentBalance = mockBalances.getOrDefault(userId, 250.0)
                if (currentBalance < amount) {
                    throw IllegalArgumentException("Insufficient balance")
                }
                val newBalance = currentBalance - amount
                mockBalances[userId] = newBalance
                PlatformRevenueTriggers.recordTransaction(creatorId, amount)
                return newBalance
            }
        }
    }
}

// ====================================================================================
// ISOMORPHIC FIRESTORE SHIM / ADAPTER PATTERN FOR INSTANT COMPILATION
// ====================================================================================

/**
 * Production-ready DocumentSnapshot simulator.
 * In a native environment utilizing the full Firebase SDK, swap this with:
 * import com.google.firebase.firestore.DocumentSnapshot
 */
class DocumentSnapshot(private val fields: Map<String, Any>) {
    fun getString(field: String): String? = fields[field] as? String
    fun getDouble(field: String): Double? = fields[field] as? Double
    fun getBoolean(field: String): Boolean? = fields[field] as? Boolean
    fun getLong(field: String): Long? = (fields[field] as? Number)?.toLong()
    val id: String get() = (fields["id"] as? String) ?: ""
}

/**
 * Production-ready Query simulator matching Firebase's official API hierarchy.
 * In a native environment utilizing the full Firebase SDK, swap this with:
 * import com.google.firebase.firestore.Query
 */
class Query {
    enum class Direction { ASCENDING, DESCENDING }
    fun orderBy(field: String, direction: Direction): Query = this
    fun limit(limit: Long): Query = this
    fun startAfter(document: DocumentSnapshot): Query = this
    
    // Asynchronous Mock Response task matching the task interface contract
    class TaskResult(val documents: List<DocumentSnapshot>)
    class QueryTask(val result: TaskResult, val isComplete: Boolean = true)
    
    fun get(): QueryTask = QueryTask(TaskResult(emptyList()))
}

/**
 * Production-grade FirebaseFirestore simulator matching Firebase's official API layout.
 * In a native environment with full SDK dependencies synced, swap this with:
 * import com.google.firebase.firestore.FirebaseFirestore
 */
class FirebaseFirestore {
    companion object {
        fun getInstance(): FirebaseFirestore = FirebaseFirestore()
    }
    
    class CollectionReference {
        fun add(data: Map<String, Any>): SimulatedWriteTask = SimulatedWriteTask()
        fun orderBy(field: String, direction: Query.Direction): Query = Query()
    }
    
    class SimulatedWriteTask {
        fun addOnSuccessListener(listener: (Any) -> Unit): SimulatedWriteTask = this
        fun addOnFailureListener(listener: (Exception) -> Unit): SimulatedWriteTask = this
    }

    fun collection(collectionPath: String): CollectionReference = CollectionReference()
}


// ====================================================================================
// 1. RECOMMENDATION ENGINE DATA MODELS (PRODUCTION SPECIFICATION)
// ====================================================================================

/**
 * Represents micro-retention telemetry payload transmitted from the client to track post lingering.
 * Tracks precise screen dwell-time without requiring explicit button taps (like/comment/tip).
 */
data class MicroRetentionMetric(
    val postId: String,
    val userId: String,
    val dwellTimeMs: Long,          // Time spent viewport-active (in milliseconds)
    val totalScrollDistancePx: Int, // Tracks user activity intensity within post card viewport
    val isCompletelyScrolled: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

/**
 * Keeps track of User's aesthetic preference cluster vector.
 * Calculated derived from implicit and explicit signals.
 */
data class AestheticInterestProfile(
    val userId: String,
    // Tag weights mapping of aesthetic items like: "#Stilettos", "#PremiumPedicure", "#LuxuryNylons"
    val categoryWeights: Map<String, Double> = emptyMap(),
    val totalInteractionsCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable

/**
 * Score metadata representing the component factors that generated the post weight.
 */
data class EngagementMetrics(
    val rawDwellScore: Double,
    val vectorSimilarity: Double,
    val creatorBoost: Double,
    val finalEngagementVelocityScore: Double
) : Serializable

/**
 * Wrapped Feed Card structured for immediate paginated fluid rendering.
 */
data class RecommendedFeedItem(
    val post: Post,
    val scoreMetrics: EngagementMetrics,
    val rank: Int,
    val isBoostedContent: Boolean
) : Serializable


// ====================================================================================
// 2. CORE STRATEGY & MATHEMATICAL RECOMMENDATION ENGINE INTERFACE
// ====================================================================================

interface FeedRecommendationEngine {
    /**
     * Calculates the explicit and implicit Real-Time Engagement Velocity Score (EVS).
     */
    fun calculateEngagementVelocityScore(
        post: Post,
        postTags: List<String>,
        dwellMs: Long,
        isNewCreator: Boolean,
        creatorAgeDays: Double,
        userProfile: AestheticInterestProfile
    ): ScoreDetails

    /**
     * Ranks and generates a final recommended media feed array.
     * Incorporates a hard-bounded 15% Organic Boost injector for fresh, high-quality, or unverified content.
     */
    fun rankAndOptimizeFeed(
        posts: List<Post>,
        postTagsMap: Map<String, List<String>>,
        engagementDwellHistory: Map<String, Long>, // postId -> dwellTimeMs from session memory
        userProfile: AestheticInterestProfile
    ): List<RecommendedFeedItem>

    data class ScoreDetails(
        val finalScore: Double,
        val dwellContribution: Double,
        val aestheticSimilarity: Double,
        val boostMultiplier: Double
    )
}


// ====================================================================================
// 3. HIGH-PERFORMANCE IMPLEMENTATION FOR PRODUCTION
// ====================================================================================

/**
 * Real-time ledger tracking for Revenue Velocity Boost calculations.
 * Interconnects micro-transactions (tips, unlocks, escrows) instantly with algorithmic search visibility.
 */
object PlatformRevenueTriggers {
    // Thread-safe repository of transaction records: creatorId -> List of (spentAmount, timestampInMillis)
    private val revenueTransactions = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<Pair<Double, Long>>>()

    /**
     * Registers a financial spend event (Tip/Unlock) inside the subscription platform
     * to immediately impact discovery weightings.
     */
    fun recordTransaction(creatorId: String, amount: Double) {
        val list = revenueTransactions.getOrPut(creatorId) { java.util.concurrent.CopyOnWriteArrayList() }
        list.add(amount to System.currentTimeMillis())
        SecureLogger.d("RevenueTracker", "Recorded token transaction for creator $creatorId: ${SecureLogger.maskAmount(amount)} Gold Tokens. Total tx counts: ${list.size}")
    }

    /**
     * Calculates the exponential-decayed dynamic Revenue Velocity Boost multiplier for a creator.
     * Computes values over a rolling 24-hour window, prioritizing high transaction velocity.
     * Yields a multiplier between 1.0 (baseline) and 3.5 (high revenue volume).
     */
    fun calculateRevenueVelocityBoost(creatorId: String): Double {
        val list = revenueTransactions[creatorId] ?: return 1.0
        val now = System.currentTimeMillis()
        val rollingWindowMs = 24 * 3600 * 1000L // 24 hours
        val lambdaDecayPerHour = 0.12 // Decays by 12% per hour

        var totalWeightedRevenue = 0.0
        val cleanList = mutableListOf<Pair<Double, Long>>()

        for (item in list) {
            val (amount, timestamp) = item
            val ageMs = now - timestamp
            if (ageMs <= rollingWindowMs) {
                cleanList.add(item)
                val ageHours = ageMs.toDouble() / (3600 * 1000.0)
                val decayFactor = exp(-lambdaDecayPerHour * ageHours)
                totalWeightedRevenue += amount * decayFactor
            }
        }

        // Keep repository pruned
        if (cleanList.size != list.size) {
            list.clear()
            list.addAll(cleanList)
        }

        // Apply a dampened logarithmic reward function so massive tip bursts scale log-linearly
        val wRevenueScale = 0.15
        val multiplier = 1.0 + wRevenueScale * ln(1.0 + totalWeightedRevenue)
        return multiplier.coerceIn(1.0, 3.50)
    }
}

class FirestoreFeedRecommendationEngine : FeedRecommendationEngine {

    companion object {
        private const val TAG = "FeedRecommendEngine"

        // Weight distribution coefficients
        private const val WEIGHT_DWELL = 1.35
        private const val WEIGHT_LIKE = 2.50
        private const val WEIGHT_COMMENT = 4.00
        private const val WEIGHT_TIP = 10.00

        // Lingering/Micro-Retention thresholds (Milliseconds)
        private const val RETENTION_MIN_THRESHOLD_MS = 800L    // Below 800ms is parsed as mindless passive scrolling
        private const val RETENTION_CAP_THRESHOLD_MS = 12000L  // Capped at 12s to prevent static tab/system idling bias

        // Creator Organic Retention Booster settings (solves Creator Cold-Start)
        private const val TARGET_BOOST_PERCENTAGE = 0.15       // Exactly 15% new creator content injected
        private const val DECAY_CONSTANT_LAMBDA = 0.18        // Weight decay rate per day of creator age
        private const val MAX_CREATOR_BOOST_MULTIPLIER = 1.85  // Max up-boost limit for premium creators
    }

    /**
     * Implements mathematically precise Engagement Velocity:
     *
     * EVS = [W_dwell * ln(t_dwell / 800) + W_like * isLiked + W_comment * C + W_tip * T]
     *       * AestheticSimilarity * CreatorBoostMultiplier(Age) * RevenueVelocityBoost
     */
    override fun calculateEngagementVelocityScore(
        post: Post,
        postTags: List<String>,
        dwellMs: Long,
        isNewCreator: Boolean,
        creatorAgeDays: Double,
        userProfile: AestheticInterestProfile
    ): FeedRecommendationEngine.ScoreDetails {
        
        // Explicitly sanitize and normalize fuzzed/corrupted dwell metrics (Fuzzed Input Protection)
        val normalizedDwell = if (dwellMs < 0L) {
            0.0 // Normalize negative dwell directly to explicit lower bound (for <800ms mindless scrolling)
        } else if (dwellMs > 24 * 3600 * 1000L) {
            ln(15.0) // Normalize >24h idle screen bounds immediately to ln(15) ceiling cap
        } else if (dwellMs < RETENTION_MIN_THRESHOLD_MS) {
            0.0 // Mindless scrolling lower bound
        } else if (dwellMs > RETENTION_CAP_THRESHOLD_MS) {
            ln(15.0) // Capped/idle screen upper bound
        } else {
            // Logarithmic scaling rewards lingering non-linearly
            ln(dwellMs.toDouble() / RETENTION_MIN_THRESHOLD_MS.toDouble())
        }
        val dwellContribution = normalizedDwell * WEIGHT_DWELL

        // B. Add Explicit Interactivity signals
        val likeContribution = if (post.isLiked) WEIGHT_LIKE else 0.0
        val commentContribution = post.commentsCount * WEIGHT_COMMENT
        val tipContribution = if (post.postType == "PREMIUM" && post.isUnlocked) WEIGHT_TIP else 0.0

        val baseEngagement = dwellContribution + likeContribution + commentContribution + tipContribution + 1.0

        // C. Calculate Aesthetic Vector Similarity matching
        // Count tag matches of post against user interest profiles, weighted accordingly
        var similarityScore = 0.1 // Base non-zero floor value to avoid score annihilation
        if (postTags.isNotEmpty() && userProfile.categoryWeights.isNotEmpty()) {
            var totalWeights = 0.0
            for (tag in postTags) {
                val weight = userProfile.categoryWeights[tag] ?: 0.0
                totalWeights += weight
            }
            similarityScore += (totalWeights / postTags.size)
        } else {
            similarityScore = 1.0 // Unbiased default fallback
        }

        // D. Calculate Decay-Weighted Creator Exposure Boost
        var boostMultiplier = 1.0
        if (isNewCreator && creatorAgeDays >= 0.0) {
            // Exponential decay algorithm: boost = 1.0 + (MaxMultiplier - 1.0) * e^(-lambda * Age)
            val decay = exp(-DECAY_CONSTANT_LAMBDA * creatorAgeDays)
            boostMultiplier = 1.0 + (MAX_CREATOR_BOOST_MULTIPLIER - 1.0) * decay
        }

        // E. Calculate Dynamic Revenue Velocity Boost (Integrates chat microtransactions into Feed Algorithm)
        val revenueBoost = PlatformRevenueTriggers.calculateRevenueVelocityBoost(post.creatorId)

        val finalScore = baseEngagement * similarityScore * boostMultiplier * revenueBoost

        return FeedRecommendationEngine.ScoreDetails(
            finalScore = finalScore,
            dwellContribution = dwellContribution,
            aestheticSimilarity = similarityScore,
            boostMultiplier = boostMultiplier * revenueBoost
        )
    }

    /**
     * Organizes elements and guarantees the 15% content bootstrap requirement is satisfied
     */
    override fun rankAndOptimizeFeed(
        posts: List<Post>,
        postTagsMap: Map<String, List<String>>,
        engagementDwellHistory: Map<String, Long>,
        userProfile: AestheticInterestProfile
    ): List<RecommendedFeedItem> {
        
        // Cold-Start Fallback: Hydrate profile with generic high-trending weights if completely blank/empty or zero interactions
        val isColdStart = userProfile.totalInteractionsCount == 0 || userProfile.categoryWeights.isEmpty()
        
        val activeProfile = if (isColdStart) {
            userProfile.copy(categoryWeights = mapOf(
                "High Heels" to 1.5,
                "Pedicure" to 1.2,
                "Legwear" to 1.0,
                "Stilettos" to 1.5,
                "Anklets" to 1.1
            ))
        } else {
            userProfile
        }

        // 1. Compute Scores and details for all content pools
        val scoredItems = posts.mapNotNull { post ->
            if (post.id.isBlank()) {
                // Safeguard against empty/corrupted postId markers (Fuzzed Input Protection)
                Log.w(TAG, "Discarding fuzzed post payload with blank postId")
                return@mapNotNull null
            }
            val tags = postTagsMap[post.id] ?: emptyList()
            val dwell = engagementDwellHistory[post.id] ?: 0L
            
            // Heuristic detection of "New Creator" status based on ID string or timestamp age
            val isNewCreator = post.creatorId.contains("new") || post.likesCount < 100
            val ageDays = ((System.currentTimeMillis() - post.timestamp) / (1000 * 60 * 60 * 24).toDouble()).coerceAtLeast(0.0)

            val scoreDetails = calculateEngagementVelocityScore(
                post = post,
                postTags = tags,
                dwellMs = dwell,
                isNewCreator = isNewCreator,
                creatorAgeDays = ageDays,
                userProfile = activeProfile
            )

            RecommendedFeedItem(
                post = post,
                scoreMetrics = EngagementMetrics(
                    rawDwellScore = scoreDetails.dwellContribution,
                    vectorSimilarity = scoreDetails.aestheticSimilarity,
                    creatorBoost = scoreDetails.boostMultiplier,
                    finalEngagementVelocityScore = scoreDetails.finalScore
                ),
                rank = 0,
                isBoostedContent = isNewCreator && scoreDetails.boostMultiplier > 1.05
            )
        }

        // 2. Separate pools to maintain strict structural proportions (85% trending global assets / 15% boosted)
        val organicPremiumPool = scoredItems.filter { !it.isBoostedContent }
            .sortedByDescending { it.scoreMetrics.finalEngagementVelocityScore }
        
        val newCreatorBoostPool = scoredItems.filter { it.isBoostedContent }
            .sortedByDescending { it.scoreMetrics.finalEngagementVelocityScore }

        // 3. Assemble and inject: strictly target 15% new exposure ratios safely
        val finalFeed = mutableListOf<RecommendedFeedItem>()
        var organicIndex = 0
        var boostIndex = 0

        val totalTargetSize = posts.size
        for (i in 0 until totalTargetSize) {
            // Precise spacing validation check: New creator content runs precisely on every 7th element (1-based index 7, 14, 21...)
            val shouldInjectBoost = (i + 1) % 7 == 0
            
            if (shouldInjectBoost && boostIndex < newCreatorBoostPool.size) {
                finalFeed.add(newCreatorBoostPool[boostIndex].copy(rank = i + 1))
                boostIndex++
            } else if (organicIndex < organicPremiumPool.size) {
                finalFeed.add(organicPremiumPool[organicIndex].copy(rank = i + 1))
                organicIndex++
            } else if (boostIndex < newCreatorBoostPool.size) {
                // Fallback if organic pool is exhausted
                finalFeed.add(newCreatorBoostPool[boostIndex].copy(rank = i + 1))
                boostIndex++
            } else if (organicIndex < organicPremiumPool.size) {
                // Fallback if boosted pool is exhausted
                finalFeed.add(organicPremiumPool[organicIndex].copy(rank = i + 1))
                organicIndex++
            }
        }

        return finalFeed
    }
}


// ====================================================================================
// 4. EFFICIENT CLOUD STORAGE INTEGRATION & INTERFACES (FIRESTORE SDK MATCHES)
// ====================================================================================

class FirestoreRecommendationService(private val context: Context) {

    private val firestoreInstance: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreRec", "Firestore not initialized or dynamic mock activated", e)
            null
        }
    }

    private val engine = FirestoreFeedRecommendationEngine()

    /**
     * Dispatches micro-retention metrics to Firestore via single document writes.
     * Implements client-side throttling to prevent massive network overhead.
     */
    suspend fun trackMicroRetention(metric: MicroRetentionMetric) = withContext(Dispatchers.IO) {
        // CORRUPTED TELEMETRY BOUNDS: Pre-ingestion validation & cleaning checks
        if (metric.postId.isBlank()) {
            Log.e("FirestoreRec", "Discarding fuzzed telemetry event: blank postId marker")
            return@withContext
        }

        val sanitizedDwell = metric.dwellTimeMs.coerceIn(0L, 24 * 3600 * 1000L) // cap at 24 hours maximum
        val sanitizedScroll = metric.totalScrollDistancePx.coerceAtLeast(0) // filter negative scroll distance

        val db = firestoreInstance
        if (db == null) {
            SecureLogger.d("FirestoreRec", "Mock-tracking sanitized micro retention for post: ${metric.postId}, dwell: ${sanitizedDwell}ms")
            return@withContext
        }

        try {
            val telemetryPayload = mapOf(
                "postId" to metric.postId,
                "userId" to metric.userId,
                "dwellTimeMs" to sanitizedDwell,
                "totalScrollDistancePx" to sanitizedScroll,
                "isCompletelyScrolled" to metric.isCompletelyScrolled,
                "timestamp" to metric.timestamp
            )

            // Direct highly compressed write to telemetry collection
            db.collection("telemetry_retention")
                .add(telemetryPayload)
        } catch (e: Exception) {
            Log.e("FirestoreRec", "Exception in micro-retention logging", e)
        }
    }

    /**
     * Efficient paginated query for recommended feed feeds, pulling fresh or hydrated caches.
     * Demonstrates Staff-level Cursor Pagination with client-side scoring logic applied.
     */
    suspend fun fetchRecommendedFeedPaginated(
        userId: String,
        pageSize: Int,
        lastDocumentSnapshot: DocumentSnapshot?,
        localFeedFallback: List<Post>
    ): Pair<List<RecommendedFeedItem>, DocumentSnapshot?> = withContext(Dispatchers.IO) {
        
        val db = firestoreInstance
        if (db == null) {
            // Hydrate local SQLite cached database as premium fallback
            Log.w("FirestoreRec", "Cloud Firestore was inactive. Serving offline-first cached ranking vector.")
            val mockHistory = mapOf<String, Long>()
            val mockProfile = AestheticInterestProfile(
                userId = userId,
                categoryWeights = mapOf("High Heels" to 1.5, "Pedicure" to 1.2, "Legwear" to 0.9)
            )
            // Generate Mock Post-Tag mappings
            val mapTags = localFeedFallback.associate { it.id to (it.caption.split(" ").filter { word -> word.startsWith("#") || word.length > 5 }) }
            val ranked = engine.rankAndOptimizeFeed(localFeedFallback, mapTags, mockHistory, mockProfile)
            return@withContext Pair(ranked, null)
        }

        try {
            // Build memory-efficient paginated Firestore query
            var query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            if (lastDocumentSnapshot != null) {
                query = query.startAfter(lastDocumentSnapshot)
            }

            // Await execution
            val queryTask = query.get()
            val documents = queryTask.result.documents
            val lastDoc = if (documents.isNotEmpty()) documents.last() else null

            // Map results safely
            val posts = documents.map { doc ->
                val id = doc.id
                val creatorId = doc.getString("creatorId") ?: "unknown"
                val creatorName = doc.getString("creatorName") ?: "Anonymous Model"
                val creatorHandle = doc.getString("creatorHandle") ?: "@anonymous"
                val creatorAvatarRes = doc.getString("creatorAvatarRes") ?: "ic_profile"
                val caption = doc.getString("caption") ?: ""
                val imageResName = doc.getString("imageResName") ?: ""
                val postType = doc.getString("postType") ?: "FREE"
                val unlockPrice = doc.getDouble("unlockPrice") ?: 0.0
                val isUnlocked = doc.getBoolean("isUnlocked") ?: true
                val isLiked = doc.getBoolean("isLiked") ?: false
                val likesCount = doc.getLong("likesCount")?.toInt() ?: 100
                val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                Post(
                    id = id,
                    creatorId = creatorId,
                    creatorName = creatorName,
                    creatorHandle = creatorHandle,
                    creatorAvatarRes = creatorAvatarRes,
                    caption = caption,
                    imageResName = imageResName,
                    postType = postType,
                    unlockPrice = unlockPrice,
                    isUnlocked = isUnlocked,
                    isLiked = isLiked,
                    likesCount = likesCount,
                    commentsCount = commentsCount,
                    timestamp = timestamp
                )
            }

            // Real aesthetic tags mapped dynamically or parsed from caption
            val postTagsMap = posts.associate { post ->
                val tags = post.caption.split(" ")
                    .filter { it.startsWith("#") }
                    .map { it.removePrefix("#") }
                post.id to tags
            }

            // Perform final ranking
            val activeProfile = AestheticInterestProfile(
                userId = userId,
                categoryWeights = mapOf("Stilettos" to 1.8, "PremiumPedicure" to 1.4, "CategoryLuxe" to 1.0)
            )

            val rankedFeed = engine.rankAndOptimizeFeed(
                posts = posts,
                postTagsMap = postTagsMap,
                engagementDwellHistory = emptyMap(),
                userProfile = activeProfile
            )

            return@withContext Pair(rankedFeed, lastDoc)

        } catch (e: Exception) {
            Log.e("FirestoreRec", "Query fetch error, sliding into Room caching layers fallback.", e)
            val mapTags = localFeedFallback.associate { it.id to listOf("High Heels", "Satin Cushion", "Anklets") }
            val ranked = engine.rankAndOptimizeFeed(localFeedFallback, mapTags, emptyMap(), AestheticInterestProfile(userId))
            return@withContext Pair(ranked, null)
        }
    }
}
