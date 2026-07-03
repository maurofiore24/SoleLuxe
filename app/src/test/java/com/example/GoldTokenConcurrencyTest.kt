package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SoleRepository
import com.example.service.GoldTokenService
import com.example.service.TransactionResult
import com.example.service.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates concurrent transactional operations on the user's wallet balance
 * using the thread-safe transactional Mutex wrapper inside GoldTokenService.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoldTokenConcurrencyTest {

    @Test
    fun testConcurrentWalletBalanceOperations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        val tokenService = GoldTokenService(repository)

        val testUserId = "concurrency_tester_user"
        
        // Setup initial balance to 150.0 Gold Tokens
        repository.updateWalletBalanceDirect(testUserId, 150.0)

        // Attempt 10 concurrent debits of 20.0 Gold Tokens each.
        // A non-transactional system with race conditions might allow all 10 to execute,
        // leading to negative balance or lost updates. Our Mutex locks enforce strict atomicity.
        val deferredJobs = (1..10).map {
            async(Dispatchers.Default) {
                tokenService.processTransaction(
                    userId = testUserId,
                    amount = 20.0,
                    type = TransactionType.UNLOCK_POST
                ) { currentBalance ->
                    // Small delay to increase the probability of thread interleaving
                    kotlinx.coroutines.delay(10)
                    true // block succeeded
                }
            }
        }

        val outcomes = deferredJobs.awaitAll()
        val successCount = outcomes.count { it is TransactionResult.Success }
        val errorCount = outcomes.count { it is TransactionResult.Error }

        // Out of 10 requests, only 7 can be afforded (7 * 20 = 140, remaining balance 10).
        // The remaining 3 must be rejected because of pre-flight checks under lock.
        Assert.assertEquals("Only 7 transactions should succeed from 150.0 balance with cost 20.0 each", 7, successCount)
        Assert.assertEquals("Remaining 3 transactions must fail due to balance depletion", 3, errorCount)

        // Inspect the final state inside the database
        val finalWallet = repository.getDirectWalletForUser(testUserId)
        Assert.assertNotNull("User wallet must still exist", finalWallet)
        Assert.assertEquals("Committed wallet balance must be exactly 10.0 coins", 10.0, finalWallet!!.balance, 0.001)
    }

    @Test
    fun testConcurrencyDoubleRefill() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        val tokenService = GoldTokenService(repository)

        val testUserId = "refill_tester_user"
        repository.updateWalletBalanceDirect(testUserId, 10.0)

        // Issue 30 concurrent refills of 15.0 Gold Tokens each.
        // The final balance must grow strictly to equal 10 + (30 * 15) = 460.0.
        val deferredRefills = (1..30).map {
            async(Dispatchers.Default) {
                tokenService.processTransaction(
                    userId = testUserId,
                    amount = 15.0,
                    type = TransactionType.REFILL_WALLET
                ) {
                    kotlinx.coroutines.delay(5)
                    true
                }
            }
        }

        deferredRefills.awaitAll()

        val finalWallet = repository.getDirectWalletForUser(testUserId)
        Assert.assertNotNull(finalWallet)
        Assert.assertEquals("Atomic concurrent refills must sum up exactly to 460.0", 460.0, finalWallet!!.balance, 0.001)
    }
}
