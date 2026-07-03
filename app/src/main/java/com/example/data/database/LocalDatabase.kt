package com.example.data.database

import android.content.Context
import androidx.room.*
import com.example.data.model.Creator
import com.example.data.model.Post
import com.example.data.model.Comment
import com.example.data.model.UserWallet
import com.example.data.model.CreatorAlbum
import com.example.data.model.CreatorMediaItem
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Participant
import kotlinx.coroutines.flow.Flow


@Dao
interface CreatorDao {
    @Query("SELECT * FROM creators ORDER BY popularityScore DESC")
    fun getAllCreators(): Flow<List<Creator>>

    @Query("SELECT * FROM creators WHERE id = :id")
    suspend fun getCreatorById(id: String): Creator?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreators(creators: List<Creator>)

    @Update
    suspend fun updateCreator(creator: Creator)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE creatorId = :creatorId ORDER BY timestamp DESC")
    fun getPostsByCreator(creatorId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Update
    suspend fun updatePost(post: Post)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}

@Dao
interface CreatorAlbumDao {
    @Query("SELECT * FROM creator_albums WHERE creatorId = :creatorId")
    fun getAlbumsByCreator(creatorId: String): Flow<List<CreatorAlbum>>

    @Query("SELECT * FROM creator_albums WHERE creatorId = :creatorId")
    suspend fun getAlbumsByCreatorDirect(creatorId: String): List<CreatorAlbum>

    @Query("SELECT * FROM creator_albums WHERE id = :id")
    suspend fun getAlbumById(id: String): CreatorAlbum?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: CreatorAlbum)

    @Update
    suspend fun updateAlbum(album: CreatorAlbum)

    @Query("DELETE FROM creator_albums WHERE id = :id")
    suspend fun deleteAlbum(id: String)
}

@Dao
interface CreatorMediaItemDao {
    @Query("SELECT * FROM creator_media WHERE creatorId = :creatorId ORDER BY timestamp DESC")
    fun getAllMediaByCreator(creatorId: String): Flow<List<CreatorMediaItem>>

    @Query("SELECT * FROM creator_media WHERE albumId = :albumId ORDER BY timestamp DESC")
    fun getMediaByAlbum(albumId: String): Flow<List<CreatorMediaItem>>

    @Query("SELECT * FROM creator_media WHERE albumId = :albumId ORDER BY timestamp DESC")
    suspend fun getMediaByAlbumDirect(albumId: String): List<CreatorMediaItem>

    @Query("SELECT * FROM creator_media WHERE id = :id")
    suspend fun getMediaItemById(id: String): CreatorMediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(mediaItem: CreatorMediaItem)

    @Update
    suspend fun updateMediaItem(mediaItem: CreatorMediaItem)

    @Query("DELETE FROM creator_media WHERE id = :id")
    suspend fun deleteMediaItem(id: String)

    @Query("DELETE FROM creator_media WHERE albumId = :albumId")
    suspend fun deleteMediaByAlbum(albumId: String)
}

@Dao
interface UserWalletDao {
    @Query("SELECT * FROM user_wallet WHERE userId = :userId")
    fun getWalletForUser(userId: String): Flow<UserWallet?>

    @Query("SELECT * FROM user_wallet WHERE userId = :userId")
    suspend fun getWalletDirectForUser(userId: String): UserWallet?

    @Query("SELECT * FROM user_wallet WHERE userId = 'local_user'")
    fun getWallet(): Flow<UserWallet?>

    @Query("SELECT * FROM user_wallet WHERE userId = 'local_user'")
    suspend fun getWalletDirect(): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWallet(wallet: UserWallet)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE tier = :tier ORDER BY timestamp DESC")
    fun getConversationsByTier(tier: String): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationDirect(conversationId: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("UPDATE messages SET status = 'READ' WHERE conversationId = :conversationId AND senderId != :currentUserId")
    suspend fun markAsRead(conversationId: String, currentUserId: String)
}

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants WHERE conversationId = :conversationId")
    suspend fun getParticipantsForConversation(conversationId: String): List<Participant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: Participant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<Participant>)
}

@Database(
    entities = [
        Creator::class, 
        Post::class, 
        Comment::class, 
        UserWallet::class,
        CreatorAlbum::class,
        CreatorMediaItem::class,
        Conversation::class,
        Message::class,
        Participant::class
    ], 
    version = 5, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creatorDao(): CreatorDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun userWalletDao(): UserWalletDao
    abstract fun creatorAlbumDao(): CreatorAlbumDao
    abstract fun creatorMediaItemDao(): CreatorMediaItemDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun participantDao(): ParticipantDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sole_luxe_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
