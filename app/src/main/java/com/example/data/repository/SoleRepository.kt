package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.Creator
import com.example.data.model.Post
import com.example.data.model.Comment
import com.example.data.model.UserWallet
import com.example.data.model.CreatorAlbum
import com.example.data.model.CreatorMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SoleRepository(context: Context) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)

    private val creatorDao = database.creatorDao()
    private val postDao = database.postDao()
    private val commentDao = database.commentDao()
    private val walletDao = database.userWalletDao()
    private val albumDao = database.creatorAlbumDao()
    private val mediaItemDao = database.creatorMediaItemDao()

    private val walletMutex = Mutex()
    private val activeOperations = ConcurrentHashMap.newKeySet<String>()

    val allCreators: Flow<List<Creator>> = creatorDao.getAllCreators()
    val allPosts: Flow<List<Post>> = postDao.getAllPosts()
    val userWallet: Flow<UserWallet?> = walletDao.getWallet()

    fun getPostsByCreator(creatorId: String): Flow<List<Post>> = postDao.getPostsByCreator(creatorId)
    fun getCommentsForPost(postId: String): Flow<List<Comment>> = commentDao.getCommentsForPost(postId)

    suspend fun updateCreator(creator: Creator) = creatorDao.updateCreator(creator)
    suspend fun insertCreators(creators: List<Creator>) = creatorDao.insertCreators(creators)

    // --- Creator Albums and Media Management ---
    fun getAlbumsByCreator(creatorId: String): Flow<List<CreatorAlbum>> = albumDao.getAlbumsByCreator(creatorId)
    suspend fun getAlbumsByCreatorDirect(creatorId: String): List<CreatorAlbum> = albumDao.getAlbumsByCreatorDirect(creatorId)
    suspend fun getAlbumById(id: String): CreatorAlbum? = albumDao.getAlbumById(id)
    suspend fun insertAlbum(album: CreatorAlbum) = albumDao.insertAlbum(album)
    suspend fun updateAlbum(album: CreatorAlbum) = albumDao.updateAlbum(album)
    suspend fun deleteAlbum(id: String) {
        albumDao.deleteAlbum(id)
        mediaItemDao.deleteMediaByAlbum(id)
    }

    fun getAllMediaByCreator(creatorId: String): Flow<List<CreatorMediaItem>> = mediaItemDao.getAllMediaByCreator(creatorId)
    fun getMediaByAlbum(albumId: String): Flow<List<CreatorMediaItem>> = mediaItemDao.getMediaByAlbum(albumId)
    suspend fun getMediaByAlbumDirect(albumId: String): List<CreatorMediaItem> = mediaItemDao.getMediaByAlbumDirect(albumId)
    suspend fun getMediaItemById(id: String): CreatorMediaItem? = mediaItemDao.getMediaItemById(id)
    suspend fun insertMediaItem(mediaItem: CreatorMediaItem) = mediaItemDao.insertMediaItem(mediaItem)
    suspend fun updateMediaItem(mediaItem: CreatorMediaItem) = mediaItemDao.updateMediaItem(mediaItem)
    suspend fun deleteMediaItem(id: String) = mediaItemDao.deleteMediaItem(id)

    suspend fun unlockCreatorAlbum(albumId: String): Boolean {
        val opKey = "unlock_album_$albumId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val album = albumDao.getAlbumById(albumId) ?: return@withTransaction false
                    if (album.isUnlocked) return@withTransaction true

                    if (album.price <= 0.0) {
                        val updatedAlbum = album.copy(isUnlocked = true)
                        albumDao.updateAlbum(updatedAlbum)
                        return@withTransaction true
                    }

                    val wallet = walletDao.getWalletDirect() ?: return@withTransaction false
                    if (wallet.balance >= album.price) {
                        val updatedWallet = wallet.copy(balance = wallet.balance - album.price)
                        walletDao.saveWallet(updatedWallet)

                        val creatorShare = album.price * 0.80
                        val platformShare = album.price - creatorShare

                        val creatorWallet = walletDao.getWalletDirectForUser(album.creatorId) ?: UserWallet(userId = album.creatorId, balance = 0.0)
                        walletDao.saveWallet(creatorWallet.copy(balance = creatorWallet.balance + creatorShare))

                        val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                        walletDao.saveWallet(platformWallet.copy(balance = platformWallet.balance + platformShare))

                        val updatedAlbum = album.copy(isUnlocked = true)
                        albumDao.updateAlbum(updatedAlbum)

                        val albumMedia = mediaItemDao.getMediaByAlbumDirect(albumId)
                        albumMedia.forEach { item ->
                            mediaItemDao.updateMediaItem(item.copy(isUnlocked = true))
                        }
                        true
                    } else {
                        false
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    suspend fun unlockCreatorMediaItem(mediaId: String): Boolean {
        val opKey = "unlock_media_$mediaId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val item = mediaItemDao.getMediaItemById(mediaId) ?: return@withTransaction false
                    if (item.isUnlocked) return@withTransaction true

                    if (item.price <= 0.0) {
                        val updatedItem = item.copy(isUnlocked = true)
                        mediaItemDao.updateMediaItem(updatedItem)
                        return@withTransaction true
                    }

                    val wallet = walletDao.getWalletDirect() ?: return@withTransaction false
                    if (wallet.balance >= item.price) {
                        val updatedWallet = wallet.copy(balance = wallet.balance - item.price)
                        walletDao.saveWallet(updatedWallet)

                        val creatorShare = item.price * 0.80
                        val platformShare = item.price - creatorShare

                        val creatorWallet = walletDao.getWalletDirectForUser(item.creatorId) ?: UserWallet(userId = item.creatorId, balance = 0.0)
                        walletDao.saveWallet(creatorWallet.copy(balance = creatorWallet.balance + creatorShare))

                        val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                        walletDao.saveWallet(platformWallet.copy(balance = platformWallet.balance + platformShare))

                        val updatedItem = item.copy(isUnlocked = true)
                        mediaItemDao.updateMediaItem(updatedItem)
                        true
                    } else {
                        false
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    // Prepopulate starting data on launch
    suspend fun checkAndPrepopulate() {
        val existingCreators = creatorDao.getAllCreators().firstOrNull()
        if (existingCreators.isNullOrEmpty()) {
            val defaultCreators = listOf(
                Creator(
                    id = "creator_anastasia",
                    name = "Anastasia Rose",
                    handle = "@anarose_heels",
                    avatarRes = "img_open_toes_pink_1782962118574",
                    bannerRes = "img_open_toes_pink_1782962118574",
                    bio = "Designer footwear modeling, luxury open-toe sandals, and completely bare feet aesthetics. Showcasing gorgeous polished nails in glossy bright colors.",
                    subPrice = 19.99,
                    subScriptionActive = false,
                    categoryTags = "Open Sandals,Barefoot,Bright Nails",
                    popularityScore = 95,
                    cryptoWalletAddress = "TYsZ8qEeeuNGeGZJPhL9asAL3n8y7qXf1T",
                    subscriptionPriceUSDT = 5.00
                ),
                Creator(
                    id = "creator_sasha",
                    name = "Sasha Blue",
                    handle = "@sashapedicure",
                    avatarRes = "img_model2_pedicure_1781222811962",
                    bannerRes = "img_model2_pedicure_1781222811962",
                    bio = "Professional pedicure artist & accessories model. Elevating nail design with exquisite foot care tips.",
                    subPrice = 14.99,
                    subScriptionActive = false,
                    categoryTags = "Pedicure,Nail Art,Satin Cushion",
                    popularityScore = 88,
                    cryptoWalletAddress = "TWh1qPZ4wWscHreUAn9tF9XQ7vDHeMyg62",
                    subscriptionPriceUSDT = 3.50
                ),
                Creator(
                    id = "creator_elena",
                    name = "Elena Wilde",
                    handle = "@elenaparty",
                    avatarRes = "img_model3_anklet_1781222826340",
                    bannerRes = "img_model3_anklet_1781222826340",
                    bio = "Gold & silver ankle jewelry model. Cozy summer style, designer beach sandals, feet aesthetics.",
                    subPrice = 24.99,
                    subScriptionActive = false,
                    categoryTags = "Anklets,Sandals,Beachwear",
                    popularityScore = 92,
                    cryptoWalletAddress = "TSpHqmYnmNffWk5fKsnL8asAL3n8y7qXfw1",
                    subscriptionPriceUSDT = 6.00
                ),
                Creator(
                    id = "creator_clara",
                    name = "Clara Lace",
                    handle = "@claralace",
                    avatarRes = "img_sexy_slippers_yellow_1782962148817",
                    bannerRes = "img_sexy_slippers_yellow_1782962148817",
                    bio = "Elegant open-back slides, sexy feather-trimmed slippers, and bright neon polished nails in cozy high-fashion settings.",
                    subPrice = 29.99,
                    subScriptionActive = false,
                    categoryTags = "Sexy Slippers,Neon Nails,Muted Luxe",
                    popularityScore = 94,
                    cryptoWalletAddress = "TEz1gWscHreUAn9tF9XQ7vDHeMyg62dfgh",
                    subscriptionPriceUSDT = 7.50
                )
            )
            creatorDao.insertCreators(defaultCreators)

            val now = System.currentTimeMillis()
            val defaultPosts = listOf(
                Post(
                    id = "post_ana_1",
                    creatorId = "creator_anastasia",
                    creatorName = "Anastasia Rose",
                    creatorHandle = "@anarose_heels",
                    creatorAvatarRes = "img_open_toes_pink_1782962118574",
                    caption = "Stepping into the evening with these gorgeous new open-toe luxury leather sandals! The bright, glossy hot pink nail polish adds the perfect touch of charm! ✨💖",
                    imageResName = "img_open_toes_pink_1782962118574",
                    postType = "FREE",
                    unlockPrice = 0.0,
                    isUnlocked = true,
                    isLiked = false,
                    likesCount = 1420,
                    commentsCount = 2,
                    timestamp = now - 3600000 * 2 // 2 hours ago
                ),
                Post(
                    id = "post_ana_premium",
                    creatorId = "creator_anastasia",
                    creatorName = "Anastasia Rose",
                    creatorHandle = "@anarose_heels",
                    creatorAvatarRes = "img_open_toes_pink_1782962118574",
                    caption = "[EXCLUSIVE PREMIUM PORTFOLIO] Detailed high-definition close-ups focusing strictly on elegant open sandals and bare feet with vibrant glossy pink nails.",
                    imageResName = "img_open_toes_pink_1782962118574",
                    postType = "PREMIUM",
                    unlockPrice = 30.0,
                    isUnlocked = false,
                    isLiked = false,
                    likesCount = 750,
                    commentsCount = 1,
                    timestamp = now - 3600000 * 24 // 1 day ago
                ),
                Post(
                    id = "post_sasha_1",
                    creatorId = "creator_sasha",
                    creatorName = "Sasha Blue",
                    creatorHandle = "@sashapedicure",
                    creatorAvatarRes = "img_model2_pedicure_1781222811962",
                    caption = "Cozy satin background pairings. This elegant pastel pedicure highlights summer breeze tones with flawless finish 💅",
                    imageResName = "img_model2_pedicure_1781222811962",
                    postType = "FREE",
                    unlockPrice = 0.0,
                    isUnlocked = true,
                    isLiked = false,
                    likesCount = 890,
                    commentsCount = 1,
                    timestamp = now - 3600000 * 5 // 5 hours ago
                ),
                Post(
                    id = "post_elena_1",
                    creatorId = "creator_elena",
                    creatorName = "Elena Wilde",
                    creatorHandle = "@elenaparty",
                    creatorAvatarRes = "img_model3_anklet_1781222826340",
                    caption = "Golden chains and double straps. Perfect geometry for sun-kissed pool layouts. Do you prefer gold or silver? 🌟",
                    imageResName = "img_model3_anklet_1781222826340",
                    postType = "FREE",
                    unlockPrice = 0.0,
                    isUnlocked = true,
                    isLiked = false,
                    likesCount = 1105,
                    commentsCount = 2,
                    timestamp = now - 3600000 * 12 // 12 hours ago
                ),
                Post(
                    id = "post_clara_1",
                    creatorId = "creator_clara",
                    creatorName = "Clara Lace",
                    creatorHandle = "@claralace",
                    creatorAvatarRes = "img_sexy_slippers_yellow_1782962148817",
                    caption = "Feeling absolute comfort and high-fashion luxury in these new yellow feather-trimmed open slippers! Perfectly finished with neon yellow polished nails. 💛✨",
                    imageResName = "img_sexy_slippers_yellow_1782962148817",
                    postType = "FREE",
                    unlockPrice = 0.0,
                    isUnlocked = true,
                    isLiked = false,
                    likesCount = 2390,
                    commentsCount = 3,
                    timestamp = now - 3600000 * 3 // 3 hours ago
                ),
                Post(
                    id = "post_clara_premium",
                    creatorId = "creator_clara",
                    creatorName = "Clara Lace",
                    creatorHandle = "@claralace",
                    creatorAvatarRes = "img_sexy_slippers_yellow_1782962148817",
                    caption = "[VIP ACCESS] Close-up luxury photography of bare feet in sexy open slippers with bright neon yellow polished toenails on cozy silk pillows.",
                    imageResName = "img_sexy_slippers_yellow_1782962148817",
                    postType = "PREMIUM",
                    unlockPrice = 45.0,
                    isUnlocked = false,
                    isLiked = false,
                    likesCount = 1205,
                    commentsCount = 0,
                    timestamp = now - 3600000 * 48 // 2 days ago
                )
            )
            postDao.insertPosts(defaultPosts)

            val defaultComments = listOf(
                Comment(
                    id = "comm_1",
                    postId = "post_ana_1",
                    authorName = "Jordan",
                    text = "The symmetry on these heels is jaw-dropping! Fantastic lighting.",
                    timestamp = now - 3600000 * 1
                ),
                Comment(
                    id = "comm_2",
                    postId = "post_ana_1",
                    authorName = "StyleCurator",
                    text = "A beautiful combination of luxury styling and exquisite posing. Love the heels!",
                    timestamp = now - 3600000 * 1 + 500000
                ),
                Comment(
                    id = "comm_3",
                    postId = "post_sasha_1",
                    authorName = "PediLover",
                    text = "This shade of blue is incredibly relaxing and beautiful! Flawless pedicures.",
                    timestamp = now - 3600000 * 4
                ),
                Comment(
                    id = "comm_4",
                    postId = "post_elena_1",
                    authorName = "AnkleLove",
                    text = "Gold chains suit the warm summer theme so beautifully!",
                    timestamp = now - 3600000 * 10
                ),
                Comment(
                    id = "comm_5",
                    postId = "post_elena_1",
                    authorName = "GildedSole",
                    text = "Amazing sandal straps. The anklet adds the perfect luxury finish.",
                    timestamp = now - 3600000 * 9
                ),
                Comment(
                    id = "comm_6",
                    postId = "post_clara_1",
                    authorName = "RetroLux",
                    text = "Sheer lace makes the modeling look like timeless Parisian high fashion.",
                    timestamp = now - 3600000 * 2
                )
            )
            for (comm in defaultComments) {
                commentDao.insertComment(comm)
            }

            // Seed beautiful swimsuit and dancing high heels poses to local creator's portfolio to avoid empty states
            val defaultMediaItems = listOf(
                CreatorMediaItem(
                    id = "media_shower_1",
                    creatorId = "local_creator",
                    albumId = null,
                    title = "Emerald Coast Shower (Turquoise Swimsuit)",
                    mediaUri = "img_beach_shower_1783035001001",
                    mediaType = "PHOTO",
                    price = 0.0,
                    isUnlocked = true,
                    timestamp = now - 3600000 * 3
                ),
                CreatorMediaItem(
                    id = "media_speedboat_1",
                    creatorId = "local_creator",
                    albumId = null,
                    title = "Yacht Club Sunset Duos (Orange & Fuchsia)",
                    mediaUri = "img_speedboat_duo_1783035002002",
                    mediaType = "PHOTO",
                    price = 45.0,
                    isUnlocked = true,
                    timestamp = now - 3600000 * 2
                ),
                CreatorMediaItem(
                    id = "media_dancer_1",
                    creatorId = "local_creator",
                    albumId = null,
                    title = "Ballroom Violet Rhythm (Dancing Uniform)",
                    mediaUri = "img_dancer_motion_1783035003003",
                    mediaType = "PHOTO",
                    price = 60.0,
                    isUnlocked = true,
                    timestamp = now - 3600000 * 1
                )
            )
            for (media in defaultMediaItems) {
                mediaItemDao.insertMediaItem(media)
            }
        }

        // Setup user wallet on first launch
        val wallet = walletDao.getWalletDirect()
        if (wallet == null) {
            walletDao.saveWallet(UserWallet(userId = "local_user", balance = 250.0))
        }
    }

    suspend fun executeSubscriptionPurchaseInRepo(
        fanId: String, 
        creatorId: String, 
        cost: Double, 
        simulateCrashAfterDeduction: Boolean = false
    ): Boolean {
        if (cost <= 0.0) {
            throw IllegalArgumentException("Cost must be strictly positive")
        }
        val opKey = "sub_purchase_${fanId}_$creatorId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val fanWallet = walletDao.getWalletDirectForUser(fanId) ?: return@withTransaction false
                    if (fanWallet.balance >= cost) {
                        // Deduct cost from fan
                        val updatedFanWallet = fanWallet.copy(balance = fanWallet.balance - cost)
                        walletDao.saveWallet(updatedFanWallet)

                        if (simulateCrashAfterDeduction) {
                            throw RuntimeException("Simulated Database Crash Mid-Transaction!")
                        }

                        // Calculate shares with precise decimal arithmetic
                        val creatorShare = cost * 0.80
                        val platformShare = cost - creatorShare

                        // Credit creator wallet
                        val creatorWallet = walletDao.getWalletDirectForUser(creatorId) ?: UserWallet(userId = creatorId, balance = 0.0)
                        val updatedCreatorWallet = creatorWallet.copy(balance = creatorWallet.balance + creatorShare)
                        walletDao.saveWallet(updatedCreatorWallet)

                        // Credit platform wallet
                        val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                        val updatedPlatformWallet = platformWallet.copy(balance = platformWallet.balance + platformShare)
                        walletDao.saveWallet(updatedPlatformWallet)

                        // Update creator subscription status
                        val creator = creatorDao.getCreatorById(creatorId)
                        if (creator != null) {
                            val updatedCreator = creator.copy(subScriptionActive = true)
                            creatorDao.updateCreator(updatedCreator)
                        }

                        true
                    } else {
                        false
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    suspend fun subscribeToCreator(creatorId: String): Boolean {
        val opKey = "subscribe_$creatorId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val creator = creatorDao.getCreatorById(creatorId) ?: return@withTransaction false
                    val wallet = walletDao.getWalletDirect() ?: return@withTransaction false

                    if (creator.subScriptionActive) {
                        // Already subscribed, unsubscribe
                        val updatedCreator = creator.copy(subScriptionActive = false)
                        creatorDao.updateCreator(updatedCreator)
                        true
                    } else {
                        if (creator.subPrice <= 0.0) {
                            throw IllegalArgumentException("Subscription price must be strictly positive")
                        }
                        // Check if user has enough balance
                        if (wallet.balance >= creator.subPrice) {
                            // Deduct coins and subscribe (using split)
                            val updatedWallet = wallet.copy(balance = wallet.balance - creator.subPrice)
                            walletDao.saveWallet(updatedWallet)

                            val creatorShare = creator.subPrice * 0.80
                            val platformShare = creator.subPrice - creatorShare

                            val creatorWallet = walletDao.getWalletDirectForUser(creatorId) ?: UserWallet(userId = creatorId, balance = 0.0)
                            walletDao.saveWallet(creatorWallet.copy(balance = creatorWallet.balance + creatorShare))

                            val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                            walletDao.saveWallet(platformWallet.copy(balance = platformWallet.balance + platformShare))

                            val updatedCreator = creator.copy(subScriptionActive = true)
                            creatorDao.updateCreator(updatedCreator)
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    suspend fun activateSubscriptionViaCrypto(creatorId: String): Boolean {
        val creator = creatorDao.getCreatorById(creatorId) ?: return false
        val updatedCreator = creator.copy(subScriptionActive = true)
        creatorDao.updateCreator(updatedCreator)
        return true
    }

    suspend fun unlockPremiumPost(postId: String): Boolean {
        val opKey = "unlock_$postId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val post = postDao.getPostById(postId) ?: return@withTransaction false
                    if (post.isUnlocked) return@withTransaction true

                    if (post.unlockPrice <= 0.0) {
                        throw IllegalArgumentException("Unlock price must be strictly positive")
                    }

                    val wallet = walletDao.getWalletDirect() ?: return@withTransaction false
                    if (wallet.balance >= post.unlockPrice) {
                        // Deduct coins and unlock with split
                        val updatedWallet = wallet.copy(balance = wallet.balance - post.unlockPrice)
                        walletDao.saveWallet(updatedWallet)

                        val creatorShare = post.unlockPrice * 0.80
                        val platformShare = post.unlockPrice - creatorShare

                        val creatorWallet = walletDao.getWalletDirectForUser(post.creatorId) ?: UserWallet(userId = post.creatorId, balance = 0.0)
                        walletDao.saveWallet(creatorWallet.copy(balance = creatorWallet.balance + creatorShare))

                        val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                        walletDao.saveWallet(platformWallet.copy(balance = platformWallet.balance + platformShare))

                        val updatedPost = post.copy(isUnlocked = true)
                        postDao.updatePost(updatedPost)
                        true
                    } else {
                        false
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    suspend fun toggleLikePost(postId: String) {
        val post = postDao.getPostById(postId) ?: return
        val currentlyLiked = post.isLiked
        val updatedPost = post.copy(
            isLiked = !currentlyLiked,
            likesCount = if (currentlyLiked) post.likesCount - 1 else post.likesCount + 1
        )
        postDao.updatePost(updatedPost)
    }

    suspend fun addComment(postId: String, text: String, authorName: String = "You") {
        if (text.isBlank()) return
        val comment = Comment(
            id = "comment_" + UUID.randomUUID().toString(),
            postId = postId,
            authorName = authorName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)

        val post = postDao.getPostById(postId)
        if (post != null) {
            val updatedPost = post.copy(commentsCount = post.commentsCount + 1)
            postDao.updatePost(updatedPost)
        }
    }

    suspend fun sendTipToCreator(creatorId: String, amount: Double): Boolean {
        if (amount <= 0.0) {
            throw IllegalArgumentException("Tip amount must be strictly positive")
        }
        val opKey = "tip_$creatorId"
        if (!activeOperations.add(opKey)) {
            return false
        }
        try {
            return walletMutex.withLock {
                database.withTransaction {
                    val wallet = walletDao.getWalletDirect() ?: return@withTransaction false
                    if (wallet.balance >= amount) {
                        // Deduct wallet balance
                        val updatedWallet = wallet.copy(balance = wallet.balance - amount)
                        walletDao.saveWallet(updatedWallet)

                        // Credit 80% to Creator, 20% to Platform
                        val creatorShare = amount * 0.80
                        val platformShare = amount - creatorShare

                        val creatorWallet = walletDao.getWalletDirectForUser(creatorId) ?: UserWallet(userId = creatorId, balance = 0.0)
                        walletDao.saveWallet(creatorWallet.copy(balance = creatorWallet.balance + creatorShare))

                        val platformWallet = walletDao.getWalletDirectForUser("admin_platform") ?: UserWallet(userId = "admin_platform", balance = 0.0)
                        walletDao.saveWallet(platformWallet.copy(balance = platformWallet.balance + platformShare))

                        // Increment popularity of model due to active support!
                        val creator = creatorDao.getCreatorById(creatorId)
                        if (creator != null) {
                            val updatedCreator = creator.copy(popularityScore = creator.popularityScore + (amount / 2).toInt())
                            creatorDao.updateCreator(updatedCreator)
                        }
                        true
                    } else {
                        false
                    }
                }
            }
        } finally {
            activeOperations.remove(opKey)
        }
    }

    suspend fun addSimulatedCoins(amount: Double) {
        val wallet = walletDao.getWalletDirect() ?: UserWallet()
        walletDao.saveWallet(wallet.copy(balance = wallet.balance + amount))
    }

    suspend fun uploadCustomPost(
        creatorId: String,
        creatorName: String,
        creatorHandle: String,
        avatarRes: String,
        caption: String,
        imageName: String,
        postType: String,
        unlockPrice: Double
    ) {
        val post = Post(
            id = "custom_" + UUID.randomUUID().toString(),
            creatorId = creatorId,
            creatorName = creatorName,
            creatorHandle = creatorHandle,
            creatorAvatarRes = avatarRes,
            caption = caption,
            imageResName = imageName,
            postType = postType,
            isUnlocked = postType == "FREE",
            unlockPrice = unlockPrice,
            isLiked = false,
            likesCount = 0,
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        postDao.insertPost(post)
    }

    suspend fun getDirectWalletForUser(userId: String): UserWallet? {
        return walletDao.getWalletDirectForUser(userId)
    }

    suspend fun updateWalletBalanceDirect(userId: String, balance: Double) {
        walletMutex.withLock {
            val existing = walletDao.getWalletDirectForUser(userId)
            val updated = existing?.copy(balance = balance) ?: UserWallet(userId = userId, balance = balance)
            walletDao.saveWallet(updated)
        }
    }
}
