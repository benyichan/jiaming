package com.xiaojiaoyin.jiaming.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xiaojiaoyin.jiaming.domain.model.Gender
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(tableName = "naming_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,   // M1 单档案；多宝宝/多方案扩展时改为自增
    val surname: String,
    val gender: String,            // MALE / FEMALE / UNKNOWN
    val traitsJson: String,        // 期望标签列表
    val avoidChars: String,        // 避讳字（连写）
    val generationChar: String?,   // 字辈字，null=未启用
    val generationAtEnd: Boolean,
    val dialectsJson: String = "[]",   // 方言 id 列表（R10）
    val birthYear: Int? = null,        // 出生信息（八字排盘；未出生为 null）
    val birthMonth: Int? = null,
    val birthDay: Int? = null,
    val birthHour: Int? = null,
) {
    fun toDomain(): NamingProfile {
        val json = Json
        return NamingProfile(
            surname = surname,
            gender = Gender.valueOf(gender),
            traits = json.decodeFromString(ListSerializer(String.serializer()), traitsJson),
            avoidChars = if (avoidChars.isBlank()) emptyList() else avoidChars.map { it.toString() },
            generationChar = generationChar,
            generationAtEnd = generationAtEnd,
            dialectIds = json.decodeFromString(ListSerializer(String.serializer()), dialectsJson),
            birthYear = birthYear,
            birthMonth = birthMonth,
            birthDay = birthDay,
            birthHour = birthHour,
        )
    }

    companion object {
        fun fromDomain(p: NamingProfile): ProfileEntity {
            val json = Json
            return ProfileEntity(
                surname = p.surname,
                gender = p.gender.name,
                traitsJson = json.encodeToString(ListSerializer(String.serializer()), p.traits),
                avoidChars = p.avoidChars.joinToString(""),
                generationChar = p.generationChar,
                generationAtEnd = p.generationAtEnd,
                dialectsJson = json.encodeToString(ListSerializer(String.serializer()), p.dialectIds),
                birthYear = p.birthYear,
                birthMonth = p.birthMonth,
                birthDay = p.birthDay,
                birthHour = p.birthHour,
            )
        }
    }
}

/** 收藏：保存候选全名与检查结果快照（ScoredCandidate 的可序列化投影） */
@kotlinx.serialization.Serializable
@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val owner: String,        // P1 / P2
    val surname: String,
    val given: String,
    val failCount: Int,
    val warnCount: Int,
    val checksJson: String,   // List<CheckResult>
    val createdAt: Long,
    val originText: String? = null,   // 典籍出处快照
    val originBook: String? = null,
    val originChapter: String? = null,
    val originGloss: String? = null,
    val finalized: Boolean = false,   // 定名标记（纪念卡/小脚印联动入口）
)

/** 家人投票登记（称呼 + 态度 + 留言），挂在「姓名+owner 的收藏」下按姓名聚合 */
@kotlinx.serialization.Serializable
@Entity(tableName = "vote")
data class VoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val given: String,
    val voter: String,       // 自定义称呼：奶奶 / 姑姑 / 我…
    val attitude: String,    // 支持 / 反对 / 待定
    val comment: String = "",
    val createdAt: Long,
)

@Dao
interface VoteDao {
    @Query("SELECT * FROM vote ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(vote: VoteEntity)

    @Query("DELETE FROM vote WHERE given = :given")
    suspend fun deleteByGiven(given: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM naming_profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM naming_profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FavoriteEntity>)

    @Query("DELETE FROM favorite WHERE surname = :surname AND given = :given AND owner = :owner")
    suspend fun delete(surname: String, given: String, owner: String)

    @Query("SELECT COUNT(*) FROM favorite WHERE surname = :surname AND given = :given AND owner = :owner")
    suspend fun countOf(surname: String, given: String, owner: String): Int

    @Query("DELETE FROM favorite")
    suspend fun clearAll()
}

@Database(entities = [ProfileEntity::class, FavoriteEntity::class, VoteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun voteDao(): VoteDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "jiaming.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
