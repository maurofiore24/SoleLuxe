package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Post
import com.example.data.repository.SoleRepository
import com.example.service.AestheticInterestProfile
import com.example.service.FirestoreFeedRecommendationEngine
import com.example.service.FirestoreRecommendationService
import com.example.service.MicroRetentionMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResilienceSecurityUnitTest {

    @Test
    fun testDoubleTapTransactionExploitGracefulRejection() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        
        // Prepopulate starting DB states
        repository.checkAndPrepopulate()
        
        // Fund user wallet to verify double spend check
        repository.addSimulatedCoins(100.0)
        
        val postId = "post_ana_premium" // Cost is 30.0 coins
        
        // Launch 5 concurrent unlock requests mimicking 50ms rapid malicious taps
        val deferredTaps = (1..5).map {
            async(Dispatchers.Default) {
                repository.unlockPremiumPost(postId)
            }
        }
        
        val outcomes = deferredTaps.awaitAll()
        val successCount = outcomes.count { it }
        
        // Verification: Exactly one transaction must succeed, subsequent double taps must be rejected safely
        Assert.assertEquals("Exactly 1 concurrent unlock request should succeed under rapid click pressure", 1, successCount)
    }

    @Test
    fun testDoubleTipConcurrencyGuard() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        repository.checkAndPrepopulate()
        repository.addSimulatedCoins(100.0)
        
        val creatorId = "creator_anastasia"
        val tipAmount = 50.0 // 1 tap is fine, 2 taps would drain 100.0 coins
        
        // Launch two rapid concurrent tipping requests (race condition vector)
        val job1 = async(Dispatchers.Default) { repository.sendTipToCreator(creatorId, tipAmount) }
        val job2 = async(Dispatchers.Default) { repository.sendTipToCreator(creatorId, tipAmount) }
        
        val results = listOf(job1.await(), job2.await())
        val balanceDeductionCount = results.count { it }
        
        // Due to "tip_creator_anastasia" activeOperations key locking, exactly one rapid request is executed
        Assert.assertEquals("Double tipping in-progress active operation lock must reject second rapid concurrent call", 1, balanceDeductionCount)
    }

    @Test
    fun testColdStartColdCacheBoundsBlankProfile() {
        val engine = FirestoreFeedRecommendationEngine()
        
        // Initialize an empty blank AestheticInterestProfile for newly registered user
        val blankProfile = AestheticInterestProfile(
            userId = "new_cold_start_user_abc",
            categoryWeights = emptyMap(),
            totalInteractionsCount = 0
        )
        
        // Mock standard Post payloads
        val posts = listOf(
            Post(
                id = "post_1",
                creatorId = "creator_1",
                creatorName = "Artist 1",
                creatorHandle = "@artist1",
                creatorAvatarRes = "ic_profile",
                caption = "Sunset #Anklets details!",
                imageResName = "img_post1",
                postType = "FREE",
                unlockPrice = 0.0,
                isUnlocked = true,
                isLiked = false,
                likesCount = 50,
                commentsCount = 2,
                timestamp = System.currentTimeMillis()
            ),
            Post(
                id = "post_2",
                creatorId = "creator_2",
                creatorName = "Artist 2",
                creatorHandle = "@artist2",
                creatorAvatarRes = "ic_profile",
                caption = "New velvet platform #HighHeels showcase",
                imageResName = "img_post2",
                postType = "FREE",
                unlockPrice = 0.0,
                isUnlocked = true,
                isLiked = false,
                likesCount = 1000,
                commentsCount = 45,
                timestamp = System.currentTimeMillis() - 3600_000
            )
        )
        
        val postTagsMap = mapOf(
            "post_1" to listOf("Anklets"),
            "post_2" to listOf("High Heels")
        )
        
        // Compute recommendation feed with zero user interactions and empty profile maps
        val recommendedList = engine.rankAndOptimizeFeed(
            posts = posts,
            postTagsMap = postTagsMap,
            engagementDwellHistory = emptyMap(),
            userProfile = blankProfile
        )
        
        // Verification: Ensure the engine does not throw division-by-zero or NPE, returning fallback hydrated list
        Assert.assertNotNull("Feed should exist", recommendedList)
        Assert.assertEquals("All posts must be mapped", 2, recommendedList.size)
        
        // Verify fallback profile weights successfully computed non-zero recommendation scores
        val topItem = recommendedList.first()
        Assert.assertTrue("Ranked item score must calculate a valid positive fallback value", topItem.scoreMetrics.finalEngagementVelocityScore > 0.0)
    }

    @Test
    fun testCorruptedTelemetryFuzzedBounds() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = FirestoreRecommendationService(context)
        
        // 1. Extreme fuzzed dwell metric > 24 hours
        val fuzzedDwellMetric = MicroRetentionMetric(
            postId = "post_1",
            userId = "test_user",
            dwellTimeMs = 36 * 3600 * 1000L, // 36 hours
            totalScrollDistancePx = 800,
            isCompletelyScrolled = true
        )
        service.trackMicroRetention(fuzzedDwellMetric) // Should clean and run safely
        
        // 2. Corrupted negative scroll distance
        val fuzzedScrollMetric = MicroRetentionMetric(
            postId = "post_1",
            userId = "test_user",
            dwellTimeMs = 4500L,
            totalScrollDistancePx = -5000, // corrupted input fuzzed value
            isCompletelyScrolled = false
        )
        service.trackMicroRetention(fuzzedScrollMetric) // Should cleanse negative to 0 and continue
        
        // 3. Blank string postId marker telemetry
        val fuzzedPostIdMetric = MicroRetentionMetric(
            postId = "", // anomalous string
            userId = "test_user",
            dwellTimeMs = 8000L,
            totalScrollDistancePx = 100,
            isCompletelyScrolled = false
        )
        service.trackMicroRetention(fuzzedPostIdMetric) // Should be safely evaluated and discarded without background thread crashes
        
        // Scoring layer validations: Let's test fuzzed inputs inside the Recommendation Engine
        val engine = FirestoreFeedRecommendationEngine()
        val defaultProfile = AestheticInterestProfile("test_user")
        
        val corruptedPost = Post(
            id = "", // blank ID token
            creatorId = "creator_1",
            creatorName = "Creator",
            creatorHandle = "@creator",
            creatorAvatarRes = "ic_profile",
            caption = "Test caption",
            imageResName = "img_1",
            postType = "FREE",
            unlockPrice = 0.0,
            isUnlocked = true,
            isLiked = false,
            likesCount = 0,
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        
        val result = engine.rankAndOptimizeFeed(
            posts = listOf(corruptedPost),
            postTagsMap = emptyMap(),
            engagementDwellHistory = emptyMap(),
            userProfile = defaultProfile
        )
        
        // Blank post id elements must be filtered and discarded safely 
        Assert.assertTrue("Blank postId posts must be omitted during evaluation", result.isEmpty())
        
        // Verify extreme dwell values are safely capped at 12s to restrict bias overflow
        val normalPost = corruptedPost.copy(id = "post_clean")
        val scoreDetailsExtra = engine.calculateEngagementVelocityScore(
            post = normalPost,
            postTags = emptyList(),
            dwellMs = 40 * 3600 * 1000L, // 40 hours
            isNewCreator = false,
            creatorAgeDays = 2.0,
            userProfile = defaultProfile
        )
        val expectedCapScore = kotlin.math.ln(12000.0 / 800.0) * 1.35
        Assert.assertEquals(expectedCapScore, scoreDetailsExtra.dwellContribution, 0.0001)
        
        // Verify negative dwell is cleared to zero score
        val scoreDetailsNegative = engine.calculateEngagementVelocityScore(
            post = normalPost,
            postTags = emptyList(),
            dwellMs = -1000L, // negative
            isNewCreator = false,
            creatorAgeDays = 2.0,
            userProfile = defaultProfile
        )
        Assert.assertEquals(0.0, scoreDetailsNegative.dwellContribution, 0.0001)
    }

    @Test
    fun testFintechWalletHandler10ThreadAssault() = kotlinx.coroutines.runBlocking {
        val walletHandler = com.example.service.FintechWalletHandler(repository = null)
        val userId = "fintech_assault_user"
        val creatorId = "creator_lux_footwear"
        
        // Setup initial balance in mock mapping to 100.0 coins
        walletHandler.mockBalances[userId] = 100.0
        
        // Attempt 10 highly parallel transaction executions.
        // If a request falls within 50ms of a previous one of the same user, it throws ConcurrentTransactionException!
        val deferredTransactions = (1..10).map { i ->
            async(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    walletHandler.executeTransaction(
                        userId = userId,
                        creatorId = creatorId,
                        amount = 10.0,
                        isPPVUnlock = true
                    )
                    true
                } catch (e: com.example.service.ConcurrentTransactionException) {
                    false
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferredTransactions.awaitAll()
        val successCount = results.count { it }
        val concurrencyExclusionCount = results.count { !it }
        
        // Since all 10 jobs are fired simultaneously and execute concurrently inside standard dispatcher,
        // our 50ms tap-clash gate throws ConcurrentTransactionException for subsequent overlapping requests.
        // Therefore, exactly 1 thread should successfully execute the transaction.
        Assert.assertEquals("Exactly 1 parallel invocation can pass the 50ms check-and-deduct gate", 1, successCount)
        Assert.assertTrue("Subsequent rapid concurrent executions are safely blocked and raise exceptions", concurrencyExclusionCount >= 9)
    }

    @Test
    fun testColdStartNPEStability() {
        val engine = com.example.service.FirestoreFeedRecommendationEngine()
        
        // Cold-start AestheticInterestProfile
        val profile = com.example.service.AestheticInterestProfile(
            userId = "cold_start_unregistered",
            categoryWeights = emptyMap(), // zero tags
            totalInteractionsCount = 0   // zero interaction metric
        )
        
        // Mocking some baseline posts (some are new creators, some are organic)
        val posts = (1..15).map { i ->
            Post(
                id = "post_$i",
                creatorId = if (i % 3 == 0) "creator_new_$i" else "creator_organic_$i",
                creatorName = "Creator $i",
                creatorHandle = "@creator$i",
                creatorAvatarRes = "ic_profile",
                caption = "Aesthetic caption #HighHeels #Anklets",
                imageResName = "img_$i",
                postType = "FREE",
                unlockPrice = 0.0,
                isUnlocked = true,
                isLiked = false,
                likesCount = if (i % 3 == 0) 10 else 500, // new creators have < 100 likes
                commentsCount = 2,
                timestamp = System.currentTimeMillis()
            )
        }
        
        val postTagsMap = posts.associate { it.id to listOf("High Heels", "Anklets") }
        
        // Process feed ranking under complete cold-start - should never throw NullPointerException!
        val optimizedFeed = engine.rankAndOptimizeFeed(
            posts = posts,
            postTagsMap = postTagsMap,
            engagementDwellHistory = emptyMap(),
            userProfile = profile
        )
        
        Assert.assertNotNull(optimizedFeed)
        Assert.assertTrue("Optimized cold-start feed must contain elements without any NPE", optimizedFeed.isNotEmpty())
    }

    @Test
    fun testCreatorSpacingRunsOnEvery7thElement() {
        val engine = com.example.service.FirestoreFeedRecommendationEngine()
        
        // Seed profile to evaluate scoring
        val profile = com.example.service.AestheticInterestProfile(
            userId = "spacing_user",
            categoryWeights = mapOf("High Heels" to 1.5),
            totalInteractionsCount = 3
        )
        
        // Create 20 posts. Make sure 15 organic and 5 are boosted new creators (using "new" creatorId suffix)
        val posts = (1..20).map { i ->
            val isBoosted = i == 7 || i == 14 || i == 15 || i == 16 || i == 17
            Post(
                id = "post_$i",
                creatorId = if (isBoosted) "new_creator" else "organic_creator",
                creatorName = "Creator $i",
                creatorHandle = "@creator$i",
                creatorAvatarRes = "ic_profile",
                caption = "Premium legwear modeling",
                imageResName = "img_$i",
                postType = "FREE",
                unlockPrice = 0.0,
                isUnlocked = true,
                isLiked = false,
                likesCount = if (isBoosted) 5 else 200, // < 100 likes qualifies as new creator
                commentsCount = 1,
                timestamp = System.currentTimeMillis()
            )
        }
        
        val postTagsMap = posts.associate { it.id to listOf("High Heels") }
        
        val resultFeed = engine.rankAndOptimizeFeed(
            posts = posts,
            postTagsMap = postTagsMap,
            engagementDwellHistory = emptyMap(),
            userProfile = profile
        )
        
        // Assert that the 7th element (index = 6), and 14th element (index = 13) are indeed boosted new creators!
        Assert.assertTrue("The 7th item (index 6) must be boosted content", resultFeed[6].isBoostedContent)
        Assert.assertTrue("The 14th item (index 13) must be boosted content", resultFeed[13].isBoostedContent)
        
        // Verify spacing assertions
        for (index in resultFeed.indices) {
            val elementRank = index + 1
            if (elementRank % 7 == 0) {
                Assert.assertTrue(
                    "Element at 1-based rank $elementRank (index $index) must be set as boosted content",
                    resultFeed[index].isBoostedContent
                )
            }
        }
    }
}
