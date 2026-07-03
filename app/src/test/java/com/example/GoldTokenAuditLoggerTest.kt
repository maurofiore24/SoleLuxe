package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SoleRepository
import com.example.service.GoldTokenAuditLogger
import com.example.service.GoldTokenService
import com.example.service.TransactionResult
import com.example.service.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoldTokenAuditLoggerTest {

    @Test
    fun testGoldTokenAuditLoggingFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SoleRepository(context)
        val auditLogger = GoldTokenAuditLogger()
        val tokenService = GoldTokenService(repository, auditLogger)

        val userId = "disputed_customer_88"

        // Setup wallet to 200
        repository.updateWalletBalanceDirect(userId, 200.0)

        // Clear pre-existing log buffers
        auditLogger.clearLogs()

        // 1. Success Transaction Log test
        val result1 = tokenService.processTransaction(
            userId = userId,
            amount = 50.0,
            type = TransactionType.UNLOCK_POST
        ) {
            true // operation block succeeded
        }

        Assert.assertTrue(result1 is TransactionResult.Success)

        // 2. Failed Transaction Log test due to Insufficient balance
        val result2 = tokenService.processTransaction(
            userId = userId,
            amount = 900.0, // way too high
            type = TransactionType.SEND_TIP
        ) {
            true
        }

        Assert.assertTrue(result2 is TransactionResult.Error)

        // 3. Aborted block callback validation
        val result3 = tokenService.processTransaction(
            userId = userId,
            amount = 10.0,
            type = TransactionType.SUBSCRIBE_CREATOR
        ) {
            false // operation block failed
        }

        Assert.assertTrue(result3 is TransactionResult.Error)

        // Read out all logs for auditing verification
        val systemLogs = auditLogger.getAuditableSystemLogs()

        Assert.assertEquals("Should have exactly 3 audited logs in registry", 3, systemLogs.size)

        // Validate details of Success entry
        val successLog = systemLogs.first { it.status == "SUCCESS" }
        Assert.assertEquals(userId, successLog.userId)
        Assert.assertEquals(50.0, successLog.amount, 0.001)
        Assert.assertEquals(TransactionType.UNLOCK_POST.name, successLog.transactionType)
        Assert.assertTrue("Transaction ID must be securely populated", successLog.transactionId.isNotBlank())
        Assert.assertTrue("Timestamp must be valid recent time", successLog.timestamp > 0)
        Assert.assertTrue(successLog.detailMessage.contains("Successfully processed"))

        // Validate details of Failed entry
        val failedLog = systemLogs.first { it.status == "FAILED" }
        Assert.assertEquals(userId, failedLog.userId)
        Assert.assertEquals(900.0, failedLog.amount, 0.001)
        Assert.assertEquals(TransactionType.SEND_TIP.name, failedLog.transactionType)
        Assert.assertTrue(failedLog.detailMessage.contains("Insufficient balance"))

        // Validate details of Aborted/Error entry
        val abortedLog = systemLogs.first { it.status == "ABORTED" }
        Assert.assertEquals(userId, abortedLog.userId)
        Assert.assertEquals(10.0, abortedLog.amount, 0.001)
        Assert.assertEquals(TransactionType.SUBSCRIBE_CREATOR.name, abortedLog.transactionType)
        Assert.assertTrue(abortedLog.detailMessage.contains("Block execution failed or aborted"))
    }
}
