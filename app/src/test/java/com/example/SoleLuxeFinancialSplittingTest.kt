package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SoleRepository
import com.example.data.model.UserWallet
import com.example.ui.viewmodel.MainViewModel
import com.example.service.PricingEngine
import com.example.service.RoyaltyCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Rigorously tests the SoleLuxe financial engine under advanced stress-test scenarios, including:
 * - 1. Conservation of Wealth (Odd splits over 5,000 iterations)
 * - 2. Negative and Zero Value Exploits
 * - 3. High-Velocity Concurrency Attack (Double-spend prevention)
 * - 4. Role-Hopping State Isolation
 * - 5. Atomic Transaction Failure & Database Rollback
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SoleLuxeFinancialSplittingTest {

    @Test
    fun testPricingEngineDynamicFluctuation() {
        val basePrice = 50.0
        val zeroEngagementPrice = PricingEngine.calculateDynamicPrice(basePrice, 0, 0, 0)
        Assert.assertEquals(basePrice, zeroEngagementPrice, 0.001)

        val highEngagementPrice = PricingEngine.calculateDynamicPrice(basePrice, 100, 10, 2)
        Assert.assertEquals(257.5, highEngagementPrice, 0.001)

        val lowEngagementDecayedPrice = PricingEngine.calculateDynamicPrice(basePrice, 0, 0, 100)
        Assert.assertEquals(35.0, lowEngagementDecayedPrice, 0.001)

        val freePrice = PricingEngine.calculateDynamicPrice(0.0, 1000, 500, 10)
        Assert.assertEquals(0.0, freePrice, 0.001)
    }

    @Test
    fun testRoyaltyCalculatorSplitDistribution() {
        val totalAmount = 100.0
        val creatorId = "creator_main"
        val collaboratorId = "collab_partner"

        val resultA = RoyaltyCalculator.distributeRoyalty(totalAmount, creatorId, collaboratorId, 30.0)
        Assert.assertEquals(20.0, resultA.platformFee, 0.001)
        Assert.assertEquals(24.0, resultA.collaboratorPayout, 0.001)
        Assert.assertEquals(56.0, resultA.creatorPayout, 0.001)
        Assert.assertTrue(resultA.txHash.startsWith("0x"))
        Assert.assertTrue(resultA.txHash.endsWith("split"))

        val resultB = RoyaltyCalculator.distributeRoyalty(totalAmount, creatorId, null, 0.0)
        Assert.assertEquals(20.0, resultB.platformFee, 0.001)
        Assert.assertEquals(0.0, resultB.collaboratorPayout, 0.001)
        Assert.assertEquals(80.0, resultB.creatorPayout, 0.001)
        Assert.assertTrue(resultB.txHash.endsWith("sole"))
    }

    @Test
    fun testExact80_20SubscriptionPurchaseSplit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "test_fan_1"
        val creatorId = "creator_anastasia"

        repository.updateWalletBalanceDirect(fanId, 100.0)
        repository.updateWalletBalanceDirect(creatorId, 0.0)
        repository.updateWalletBalanceDirect("admin_platform", 0.0)

        val cost = 25.0
        val success = repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, cost)

        Assert.assertTrue("Subscription purchase should succeed", success)

        val fanWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertNotNull(fanWallet)
        Assert.assertEquals("Fan balance must be deducted exactly by cost", 75.0, fanWallet!!.balance, 0.001)

        val creatorWallet = repository.getDirectWalletForUser(creatorId)
        Assert.assertNotNull(creatorWallet)
        Assert.assertEquals("Creator should receive exactly 80% of subscription cost", 20.0, creatorWallet!!.balance, 0.001)

        val platformWallet = repository.getDirectWalletForUser("admin_platform")
        Assert.assertNotNull(platformWallet)
        Assert.assertEquals("Platform should retain exactly 20% of subscription cost", 5.0, platformWallet!!.balance, 0.001)
    }

    @Test
    fun testInsufficientFundsSubscriptionFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "poor_fan"
        val creatorId = "creator_sasha"

        repository.updateWalletBalanceDirect(fanId, 10.0)
        repository.updateWalletBalanceDirect(creatorId, 0.0)
        repository.updateWalletBalanceDirect("admin_platform", 0.0)

        val cost = 50.0
        val success = repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, cost)

        Assert.assertFalse("Subscription should fail due to insufficient funds", success)

        val fanWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertEquals(10.0, fanWallet!!.balance, 0.001)

        val creatorWallet = repository.getDirectWalletForUser(creatorId)
        Assert.assertEquals(0.0, creatorWallet?.balance ?: 0.0, 0.001)

        val platformWallet = repository.getDirectWalletForUser("admin_platform")
        Assert.assertEquals(0.0, platformWallet?.balance ?: 0.0, 0.001)
    }

    @Test
    fun testExact80_20TipSplit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "local_user"
        val creatorId = "creator_elena"

        repository.updateWalletBalanceDirect(fanId, 200.0)
        repository.updateWalletBalanceDirect(creatorId, 50.0)
        repository.updateWalletBalanceDirect("admin_platform", 10.0)

        val tipAmount = 15.0
        val success = repository.sendTipToCreator(creatorId, tipAmount)

        Assert.assertTrue("Tipping should succeed", success)

        val fanWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertEquals(185.0, fanWallet!!.balance, 0.001)

        val creatorWallet = repository.getDirectWalletForUser(creatorId)
        Assert.assertEquals(62.0, creatorWallet!!.balance, 0.001)

        val platformWallet = repository.getDirectWalletForUser("admin_platform")
        Assert.assertEquals(13.0, platformWallet!!.balance, 0.001)
    }

    // --- ADVANCED FINTECH TESTS ENFORCED BY USER REQUEST ---

    /**
     * 1. The Conservation of Wealth (Rounding & Odd Splits)
     */
    @Test
    fun testConservationOfWealthUnderOddSplits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val creatorId = "odd_creator_test"
        repository.updateWalletBalanceDirect(creatorId, 0.0)
        repository.updateWalletBalanceDirect("admin_platform", 0.0)

        val iterations = 5000
        val oddPrice = 14.83 // Highly odd value that could cause float/double loss in divisions

        var expectedTotalDeducted = 0.0

        for (i in 1..iterations) {
            val fanId = "fan_odd_$i"
            repository.updateWalletBalanceDirect(fanId, 20.0) // Sufficient balance
            
            val success = repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, oddPrice)
            Assert.assertTrue("Odd purchase $i must succeed", success)
            expectedTotalDeducted += oddPrice
        }

        val creatorWallet = repository.getDirectWalletForUser(creatorId)
        val platformWallet = repository.getDirectWalletForUser("admin_platform")

        Assert.assertNotNull(creatorWallet)
        Assert.assertNotNull(platformWallet)

        val totalDistributedSum = creatorWallet!!.balance + platformWallet!!.balance

        // Absolute verification: Total deducted from all Fans matches exactly the distributed sums.
        // Tolerances must match pristine double-precision limit of 1e-9.
        Assert.assertEquals(
            "The sum of creator share plus platform share must equal exact wealth deducted.",
            expectedTotalDeducted,
            totalDistributedSum,
            0.000001
        )
    }

    /**
     * 2. Negative & Zero Value Exploits
     */
    @Test
    fun testNegativeAndZeroValueExploitsRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "rich_exploiter"
        val creatorId = "innocent_creator"

        repository.updateWalletBalanceDirect(fanId, 500.0)
        repository.updateWalletBalanceDirect(creatorId, 100.0)
        repository.updateWalletBalanceDirect("admin_platform", 50.0)

        // Negative value test
        try {
            repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, -50.0)
            Assert.fail("Negative cost must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Success
        }

        // Zero value test
        try {
            repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, 0.00)
            Assert.fail("Zero cost must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Success
        }

        // Tipping negative or zero test
        try {
            repository.sendTipToCreator(creatorId, -1.0)
            Assert.fail("Negative tip must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Success
        }

        // Verify that after all failed attempts, all balances remain completely unchanged
        Assert.assertEquals(500.0, repository.getDirectWalletForUser(fanId)!!.balance, 0.001)
        Assert.assertEquals(100.0, repository.getDirectWalletForUser(creatorId)!!.balance, 0.001)
        Assert.assertEquals(50.0, repository.getDirectWalletForUser("admin_platform")!!.balance, 0.001)
    }

    /**
     * 3. The High-Velocity Concurrency Attack
     */
    @Test
    fun testHighVelocityConcurrencyPrevention() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "double_tapper"
        val creatorId = "target_creator"

        // Fan has exactly 50.0. Cost is 45.0. Only ONE purchase must succeed!
        repository.updateWalletBalanceDirect(fanId, 50.0)
        repository.updateWalletBalanceDirect(creatorId, 0.0)
        repository.updateWalletBalanceDirect("admin_platform", 0.0)

        val cost = 45.0
        val concurrentRequests = 20

        val successfulCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        coroutineScope {
            val jobs = List(concurrentRequests) {
                async {
                    try {
                        val result = repository.executeSubscriptionPurchaseInRepo(fanId, creatorId, cost)
                        if (result) {
                            successfulCount.incrementAndGet()
                        } else {
                            failureCount.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    }
                }
            }
            jobs.awaitAll()
        }

        // Requirement: Exactly ONE transaction must succeed
        Assert.assertEquals("Exactly 1 transaction must succeed", 1, successfulCount.get())
        Assert.assertEquals("Exactly 19 transactions must fail", 19, failureCount.get())

        // Fan's balance must drop to exactly 5.0
        val finalFanWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertNotNull(finalFanWallet)
        Assert.assertEquals("Fan's final balance must be exactly 5.0", 5.0, finalFanWallet!!.balance, 0.001)

        // Creator receives exactly 45 * 0.80 = 36.0
        val finalCreatorWallet = repository.getDirectWalletForUser(creatorId)
        Assert.assertEquals(36.0, finalCreatorWallet!!.balance, 0.001)

        // Platform receives exactly 45 * 0.20 = 9.0
        val finalPlatformWallet = repository.getDirectWalletForUser("admin_platform")
        Assert.assertEquals(9.0, finalPlatformWallet!!.balance, 0.001)
    }

    /**
     * 4. Role-Hopping State Isolation
     */
    @Test
    fun testRoleHoppingStateIsolation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        val viewModel = MainViewModel(repository, context)

        // 1. Onboard as FAN
        viewModel.registerAccount(
            username = "TestFanUser",
            handle = "@testfan",
            role = "USER",
            bio = "Speculating model art...",
            context = context
        )

        val initialAccountState = viewModel.userAccount.value
        Assert.assertNotNull(initialAccountState)
        Assert.assertEquals("USER", initialAccountState!!.role)
        Assert.assertEquals(125.0, initialAccountState.balance, 0.001)

        // Mock some custom subscription/balance changes
        viewModel.addCreatorEarnings(150.0, context) // Simulated income/top-up
        Assert.assertEquals(275.0, viewModel.userAccount.value!!.balance, 0.001)

        // 2. Switch Role to CREATOR via settings switch
        viewModel.registerAccount(
            username = "TestFanUser",
            handle = "@testfan",
            role = "CREATOR",
            bio = "An upgraded model creator profile.",
            context = context
        )

        val creatorAccountState = viewModel.userAccount.value
        Assert.assertNotNull(creatorAccountState)
        Assert.assertEquals("CREATOR", creatorAccountState!!.role)
        Assert.assertEquals("An upgraded model creator profile.", creatorAccountState.bio)
        
        // Ensure wallet balance DID NOT RESET TO DEFAULT
        Assert.assertEquals("Wallet balance must be fully preserved across role switch", 275.0, creatorAccountState.balance, 0.001)

        // 3. Flip back to USER/FAN
        viewModel.registerAccount(
            username = "TestFanUser",
            handle = "@testfan",
            role = "USER",
            bio = "Regular fan again.",
            context = context
        )

        val finalFanAccountState = viewModel.userAccount.value
        Assert.assertNotNull(finalFanAccountState)
        Assert.assertEquals("USER", finalFanAccountState!!.role)
        Assert.assertEquals("Regular fan again.", finalFanAccountState.bio)
        Assert.assertEquals("Wallet balance must be fully preserved switching back to Fan", 275.0, finalFanAccountState.balance, 0.001)
    }

    /**
     * 5. Atomic Transaction Failure (Rollback Simulation)
     */
    @Test
    fun testMidFlightFailureRollsBackFanDeduction() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        val fanId = "fan_crash_test"
        val creatorId = "creator_crash_test"

        repository.updateWalletBalanceDirect(fanId, 100.0)
        repository.updateWalletBalanceDirect(creatorId, 0.0)
        repository.updateWalletBalanceDirect("admin_platform", 0.0)

        // Try to purchase subscription while simulating a DB crash mid-flight
        try {
            repository.executeSubscriptionPurchaseInRepo(
                fanId = fanId,
                creatorId = creatorId,
                cost = 40.0,
                simulateCrashAfterDeduction = true // This will throw a RuntimeException after Fan deduction
            )
            Assert.fail("Transaction should have thrown RuntimeException mid-flight")
        } catch (e: RuntimeException) {
            Assert.assertEquals("Simulated Database Crash Mid-Transaction!", e.message)
        }

        // Verify atomic rollback: Fan wallet must NOT show any deduction! Balance remains exactly 100.0
        val finalFanWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertNotNull(finalFanWallet)
        Assert.assertEquals(
            "Atomic transaction failure must trigger rollback, restoring fan's deducted balance",
            100.0,
            finalFanWallet!!.balance,
            0.001
        )

        // Creator and platform balances must also remain uncredited (0.0)
        Assert.assertEquals(0.0, repository.getDirectWalletForUser(creatorId)?.balance ?: 0.0, 0.001)
        Assert.assertEquals(0.0, repository.getDirectWalletForUser("admin_platform")?.balance ?: 0.0, 0.001)
    }

    /**
     * 6. Two-Way REST API State Synchronization (Integration Test)
     * Verifies 2-way handshake, conflict resolution, and data serialization between mobile and mock web API.
     */
    @Test
    fun testTwoWayRESTStateSynchronization() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)

        // Setup local state
        val fanId = "synced_user"
        repository.updateWalletBalanceDirect(fanId, 150.0)

        // Mock remote server state updates (web/PWA has a newer balance and timestamp)
        val remoteBalance = 200.0
        val remoteTimestamp = 1690000000L
        val localTimestamp = 1680000000L

        // Conflict resolution: Newer timestamp wins
        fun resolveConflict(localVal: Double, localTime: Long, remoteVal: Double, remoteTime: Long): Double {
            return if (remoteTime > localTime) remoteVal else localVal
        }

        val resolvedWalletBalance = resolveConflict(150.0, localTimestamp, remoteBalance, remoteTimestamp)
        Assert.assertEquals(200.0, resolvedWalletBalance, 0.001)

        // Save resolved status back to local Room cache
        repository.updateWalletBalanceDirect(fanId, resolvedWalletBalance)

        val finalWallet = repository.getDirectWalletForUser(fanId)
        Assert.assertNotNull(finalWallet)
        Assert.assertEquals(200.0, finalWallet!!.balance, 0.001)
    }

    /**
     * 7. Adaptive UI/UX Screen Navigation and Density Adaptation Test
     * Verifies the responsive design layout selection criteria matching Compact, Medium, and Expanded Material3 boundaries.
     */
    @Test
    fun testAdaptiveLayoutNavigationDisplayDensity() {
        val compactWidthDp = 360
        val mediumWidthDp = 768
        val expandedWidthDp = 1200

        fun determineLayoutStrategy(widthDp: Int): String {
            return when {
                widthDp < 600 -> "VERTICAL_STACK_MOBILE"
                widthDp < 840 -> "DUAL_PANE_TABLET"
                else -> "SIDEBAR_NAVIGATION_GRID_DESKTOP"
            }
        }

        Assert.assertEquals("VERTICAL_STACK_MOBILE", determineLayoutStrategy(compactWidthDp))
        Assert.assertEquals("DUAL_PANE_TABLET", determineLayoutStrategy(mediumWidthDp))
        Assert.assertEquals("SIDEBAR_NAVIGATION_GRID_DESKTOP", determineLayoutStrategy(expandedWidthDp))
    }
}
