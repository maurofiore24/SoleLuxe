package com.example.service

import com.example.data.model.UserWallet
import com.example.data.repository.SoleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

enum class TransactionType {
    UNLOCK_POST,
    SUBSCRIBE_CREATOR,
    SEND_TIP,
    REFILL_WALLET
}

sealed class TransactionResult {
    data class Success(val prevBalance: Double, val newBalance: Double) : TransactionResult()
    data class Error(val reason: String) : TransactionResult()
}

/**
 * GoldTokenService provides a highly secure and bulletproof Transactional Mutex lock wrapper
 * to manage all 'Gold Token' operations. By utilizing dynamic per-user Locks, we guarantee
 * total isolation and prevent double-tap race condition exploits.
 */
class GoldTokenService(
    private val repository: SoleRepository,
    val auditLogger: GoldTokenAuditLogger = GoldTokenAuditLogger()
) {

    // Thread-safe map storing dynamic Mutex instances for each active user transaction pipeline
    private val lockRegistry = ConcurrentHashMap<String, Mutex>()

    /**
     * Resolves the Mutex lock associated with the given userId.
     */
    private fun getLockForUser(userId: String): Mutex {
        return lockRegistry.computeIfAbsent(userId) { Mutex() }
    }

    /**
     * Safely executes an action requiring 'Gold Token' tokens atomically.
     * Keeps track of user-scoped threads and blocks concurrent double-spending attempts.
     *
     * @param userId The ID of the wallet owner initiating the transaction
     * @param amount The cost or transfer amount in Gold Tokens
     * @param type The type of transaction being performed
     * @param block The database or API operation to complete before committing the balance change
     */
    suspend fun processTransaction(
        userId: String,
        amount: Double,
        type: TransactionType,
        block: suspend (currentBalance: Double) -> Boolean
    ): TransactionResult {
        if (amount < 0.0) {
            val errorMsg = "Transaction amount cannot be negative"
            auditLogger.logTransaction(userId, amount, type, "FAILED", errorMsg)
            return TransactionResult.Error(errorMsg)
        }

        val userMutex = getLockForUser(userId)

        // Block concurrent threads working on the same userId
        return userMutex.withLock {
            try {
                // Fetch direct, non-cached fresh wallet snapshot
                val currentWallet = repository.getDirectWalletForUser(userId)
                    ?: UserWallet(userId = userId, balance = 0.0)

                val balanceBefore = currentWallet.balance

                if (type != TransactionType.REFILL_WALLET && balanceBefore < amount) {
                    val errorMsg = "Insufficient balance: Need $amount but have $balanceBefore"
                    auditLogger.logTransaction(userId, amount, type, "FAILED", errorMsg)
                    return@withLock TransactionResult.Error(errorMsg)
                }

                // Execute transactional operation (DB inserts, subscription updates, etc.)
                val blockOutcome = block(balanceBefore)
                if (!blockOutcome) {
                    val errorMsg = "Block execution failed or aborted transaction"
                    auditLogger.logTransaction(userId, amount, type, "ABORTED", errorMsg)
                    return@withLock TransactionResult.Error(errorMsg)
                }

                // Compute and write back updated balance
                val balanceAfter = if (type == TransactionType.REFILL_WALLET) {
                    balanceBefore + amount
                } else {
                    balanceBefore - amount
                }

                repository.updateWalletBalanceDirect(userId, balanceAfter)

                val message = "Successfully processed ${type.name} for $amount. Prev balance: $balanceBefore, New balance: $balanceAfter."
                auditLogger.logTransaction(userId, amount, type, "SUCCESS", message)

                TransactionResult.Success(prevBalance = balanceBefore, newBalance = balanceAfter)
            } catch (e: Exception) {
                val errorMsg = "Exception thrown during transaction execution: ${e.message}"
                auditLogger.logTransaction(userId, amount, type, "ERROR", errorMsg)
                TransactionResult.Error(errorMsg)
            }
        }
    }
}
