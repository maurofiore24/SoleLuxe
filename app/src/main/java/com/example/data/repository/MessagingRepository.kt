package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Participant
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MessagingRepository(context: Context) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val participantDao = database.participantDao()

    suspend fun sendMessage(message: Message) {
        // Insert message locally
        messageDao.insertMessage(message)

        // Find or create/update conversation
        val existing = conversationDao.getConversationById(message.conversationId)
        if (existing != null) {
            val updated = existing.copy(
                lastMessage = if (message.isPremium && !message.isUnlocked) "[Premium Attachment - Locked]" else message.text,
                timestamp = message.timestamp,
                unreadCount = existing.unreadCount + 1
            )
            conversationDao.updateConversation(updated)
        } else {
            // Unsolicited messages default to REQUESTS tier
            val isFromCreator = message.senderId.startsWith("creator_")
            val defaultTier = if (isFromCreator) "PRIMARY" else "REQUESTS"
            val defaultHeld = !isFromCreator // Default hold for unverified fan DMs to protect creators

            val newConv = Conversation(
                id = message.conversationId,
                title = message.senderName,
                lastMessage = if (message.isPremium && !message.isUnlocked) "[Premium Attachment - Locked]" else message.text,
                unreadCount = 1,
                timestamp = message.timestamp,
                isLocked = false,
                isHeld = defaultHeld,
                tier = defaultTier
            )
            conversationDao.insertConversation(newConv)
        }
    }

    fun getConversationsForUser(userId: String): Flow<List<Conversation>> {
        // Return conversations, sorted by timestamp via database query
        return conversationDao.getAllConversations()
    }

    fun getConversationsByTier(tier: String): Flow<List<Conversation>> {
        return conversationDao.getConversationsByTier(tier)
    }

    suspend fun getConversationById(conversationId: String): Conversation? {
        return conversationDao.getConversationById(conversationId)
    }

    suspend fun insertConversationDirect(conversation: Conversation) {
        conversationDao.insertConversation(conversation)
    }

    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message)
    }

    suspend fun markAsRead(conversationId: String) {
        messageDao.markAsRead(conversationId, "local_user")
        val conversation = conversationDao.getConversationById(conversationId)
        if (conversation != null) {
            conversationDao.updateConversation(conversation.copy(unreadCount = 0))
        }
    }

    fun streamMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun setConversationHeld(conversationId: String, isHeld: Boolean) {
        val conversation = conversationDao.getConversationById(conversationId)
        if (conversation != null) {
            conversationDao.updateConversation(conversation.copy(isHeld = isHeld))
        }
    }

    suspend fun insertParticipantDirect(participant: Participant) {
        participantDao.insertParticipant(participant)
    }

    suspend fun prepopulateDefaultConversations() {
        val count = conversationDao.getConversationById("conv_anastasia")
        if (count == null) {
            // Anastasia - PRIMARY subscriber
            val c1 = Conversation(
                id = "conv_anastasia",
                title = "Anastasia Rose",
                lastMessage = "I just took a secret sunset pose with my new pink open-toe luxury leather sandals. Let me know if you want to unlock it! ✨",
                unreadCount = 1,
                timestamp = System.currentTimeMillis() - 100000,
                isLocked = false,
                isHeld = false,
                tier = "PRIMARY"
            )
            conversationDao.insertConversation(c1)
            participantDao.insertParticipants(listOf(
                Participant("conv_anastasia_local", "conv_anastasia", "local_user", "FAN"),
                Participant("conv_anastasia_creator", "conv_anastasia", "creator_anastasia", "CREATOR")
            ))
            messageDao.insertMessage(Message("m1_1", "conv_anastasia", "creator_anastasia", "Anastasia Rose", "img_open_toes_pink_1782962118574", "Hello gorgeous! Ready for some luxury open-toe sandals today? 👡", System.currentTimeMillis() - 300000))
            messageDao.insertMessage(Message("m1_2", "conv_anastasia", "creator_anastasia", "Anastasia Rose", "img_open_toes_pink_1782962118574", "I just took a secret sunset pose with my new pink open-toe luxury leather sandals. Let me know if you want to unlock it! ✨", System.currentTimeMillis() - 200000))
            messageDao.insertMessage(Message("m1_3", "conv_anastasia", "creator_anastasia", "Anastasia Rose", "img_open_toes_pink_1782962118574", "Exclusive content piece", System.currentTimeMillis() - 100000, mediaUri = "img_open_toes_pink_1782962118574", isPremium = true, unlockPriceCoins = 60.0, isUnlocked = false))

            // Sasha - PRIMARY subscriber
            val c2 = Conversation(
                id = "conv_sasha",
                title = "Sasha Blue",
                lastMessage = "Welcome to my pedicure sanctuary! What aesthetic tones are we feeling? 💅",
                unreadCount = 0,
                timestamp = System.currentTimeMillis() - 200000,
                isLocked = false,
                isHeld = false,
                tier = "PRIMARY"
            )
            conversationDao.insertConversation(c2)
            participantDao.insertParticipants(listOf(
                Participant("conv_sasha_local", "conv_sasha", "local_user", "FAN"),
                Participant("conv_sasha_creator", "conv_sasha", "creator_sasha", "CREATOR")
            ))
            messageDao.insertMessage(Message("m2_1", "conv_sasha", "creator_sasha", "Sasha Blue", "img_model2_pedicure_1781222811962", "Welcome to my pedicure sanctuary! What aesthetic tones are we feeling? 💅", System.currentTimeMillis() - 200000))

            // Unverified fan - REQUESTS and HELD (Spam protection)
            val c3 = Conversation(
                id = "conv_unverified_fan",
                title = "Unverified User Request",
                lastMessage = "Hey, I wanted to order a custom photo of yellow feather slippers! Will pay extra gold.",
                unreadCount = 1,
                timestamp = System.currentTimeMillis() - 50000,
                isLocked = false,
                isHeld = true, // Spam protection is ACTIVE (Held by default)
                tier = "REQUESTS"
            )
            conversationDao.insertConversation(c3)
            participantDao.insertParticipants(listOf(
                Participant("conv_unverified_local", "conv_unverified_fan", "local_user", "CREATOR"),
                Participant("conv_unverified_other", "conv_unverified_fan", "unverified_fan_101", "FAN")
            ))
            messageDao.insertMessage(Message("m3_1", "conv_unverified_fan", "unverified_fan_101", "Unverified Fan", "img_model4_stockings_1781222837876", "Hey, I wanted to order a custom photo of yellow feather slippers! Will pay extra gold.", System.currentTimeMillis() - 50000))
        }
    }
}
