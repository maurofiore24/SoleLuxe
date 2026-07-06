package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "creators")
data class Creator(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String,
    val avatarRes: String, // name of the drawable resource, e.g. "img_model1_highheels_1781222793764"
    val bannerRes: String,
    val bio: String,
    val subPrice: Double, // monthly sub price in virtual gold coins
    val subScriptionActive: Boolean, // if user has subscribed
    val categoryTags: String, // e.g. "Heels,Pedicures,Anklets"
    val popularityScore: Int,
    val verified: Boolean = true,
    val cryptoWalletAddress: String = "",
    val subscriptionPriceUSDT: Double = 5.00
) : Serializable

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorAvatarRes: String,
    val caption: String,
    val imageResName: String, // pointing to our generated drawables, e.g. "img_model1_highheels_1781222793764"
    val postType: String, // "FREE" or "PREMIUM"
    val unlockPrice: Double, // price to unlock if PREMIUM
    val isUnlocked: Boolean, // true if FREE or if premium purchased
    val isLiked: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val timestamp: Long,
    
    // Strategic additions
    val collaboratorId: String? = null,
    val collaboratorSplit: Double = 0.0, // Percentage split, e.g. 50.0 for 50%
    val isPreOrder: Boolean = false,
    val releaseTimestamp: Long = 0L,
    val isPrePurchased: Boolean = false,
    val arOverlayType: String? = null, // e.g. "HEELS", "ANKLET", "STOCKINGS"
    val arOverlayIntensity: Float = 1.0f
) : Serializable

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val postId: String,
    val authorName: String,
    val text: String,
    val timestamp: Long
) : Serializable

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val userId: String = "local_user",
    val balance: Double = 250.0 // starts with some virtual gold coins to play around with premium content!
) : Serializable

@Entity(tableName = "creator_albums")
data class CreatorAlbum(
    @PrimaryKey val id: String,
    val creatorId: String,
    val name: String,
    val description: String,
    val price: Double, // Price in virtual gold coins to unlock the entire album (0.0 if free)
    val isUnlocked: Boolean = false,
    val coverRes: String = "", // Placeholder theme or system uri
    
    // Strategic additions
    val collaboratorId: String? = null,
    val collaboratorSplit: Double = 0.0,
    val isPreOrder: Boolean = false,
    val releaseTimestamp: Long = 0L,
    val isPrePurchased: Boolean = false
) : Serializable

@Entity(tableName = "creator_media")
data class CreatorMediaItem(
    @PrimaryKey val id: String,
    val creatorId: String,
    val albumId: String?, // null if not in any album (loose content)
    val title: String,
    val mediaUri: String, // point to picked content or a simulated aesthetic photo asset
    val mediaType: String, // "PHOTO" or "VIDEO"
    val price: Double, // Individual media unlock price (0.0 if free)
    val isUnlocked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Strategic additions
    val collaboratorId: String? = null,
    val collaboratorSplit: Double = 0.0,
    val isPreOrder: Boolean = false,
    val releaseTimestamp: Long = 0L,
    val isPrePurchased: Boolean = false,
    val arOverlayType: String? = null, // e.g. "HEELS", "ANKLET", "STOCKINGS"
    val arOverlayIntensity: Float = 1.0f
) : Serializable

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val unreadCount: Int,
    val timestamp: Long,
    val isLocked: Boolean = false,
    val isHeld: Boolean = false, // Spam Protection: true if Creator holds/filters this unverified user
    val tier: String = "PRIMARY" // "PRIMARY" (Subscribers/Collabs), "REQUESTS" (Unverified/Paid DMs), "ARCHIVED"
) : Serializable

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: Long,
    val mediaUri: String? = null,
    val isPremium: Boolean = false,
    val unlockPriceCoins: Double = 0.0,
    val isUnlocked: Boolean = true,
    val status: String = "SENT" // "SENT", "DELIVERED", "READ"
) : Serializable

@Entity(tableName = "participants")
data class Participant(
    @PrimaryKey val id: String, // format: "conversationId_userId"
    val conversationId: String,
    val userId: String,
    val role: String = "FAN" // "CREATOR", "FAN", "COLLAB"
) : Serializable
