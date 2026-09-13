package com.xiaojiaoyin.jiaming.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xiaojiaoyin.jiaming.data.AssetsDataSource
import com.xiaojiaoyin.jiaming.data.db.AppDatabase
import com.xiaojiaoyin.jiaming.data.db.FavoriteEntity
import com.xiaojiaoyin.jiaming.data.db.ProfileEntity
import com.xiaojiaoyin.jiaming.data.db.VoteEntity
import com.xiaojiaoyin.jiaming.data.prefs.SettingsStore
import com.xiaojiaoyin.jiaming.domain.NamingEngine
import com.xiaojiaoyin.jiaming.domain.model.CheckResult
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import com.xiaojiaoyin.jiaming.domain.model.ScoredCandidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class ExportBundle(
    val app: String = "jiaming",
    val version: Int = 1,
    val profile: ProfileDto? = null,
    val favorites: List<FavoriteEntity> = emptyList(),
    val votes: List<VoteEntity> = emptyList(),
)

/** 档案导出 DTO（不暴露 Room entity 的全部细节） */
@Serializable
data class ProfileDto(
    val surname: String,
    val gender: String,
    val traits: List<String>,
    val avoidChars: String,
    val generationChar: String? = null,
    val generationAtEnd: Boolean = false,
)

/**
 * 全局状态枢纽：档案、候选、收藏。
 * 生成计算放 Dispatchers.Default（字库组合 ~6 万次过管线，中端机实测可秒级完成）。
 */
class MainViewModel(
    val assets: AssetsDataSource,
    private val db: AppDatabase,
    val settings: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { ignoreUnknownKeys = true }
    val baziEngine = com.xiaojiaoyin.jiaming.domain.bazi.BaziEngine()

    val profileFlow: Flow<ProfileEntity?> = db.profileDao().observe()
    val favoritesFlow: Flow<List<FavoriteEntity>> = db.favoriteDao().observeAll()
    val votesFlow: Flow<List<VoteEntity>> = db.voteDao().observeAll()

    var candidates by mutableStateOf<List<ScoredCandidate>>(emptyList())
        private set
    var generating by mutableStateOf(false)
        private set
    var canShuffle by mutableStateOf(false)
        private set

    private var rankedCache: List<ScoredCandidate> = emptyList()

    fun saveProfile(p: NamingProfile) {
        scope.launch { db.profileDao().upsert(ProfileEntity.fromDomain(p)) }
    }

    fun generate(p: NamingProfile) {
        scope.launch {
            generating = true
            // 重计算只做一次（秒级）；「换一批」在缓存上毫秒级重新采样
            rankedCache = withContext(Dispatchers.Default) { assets.engine.rank(p) }
            generating = false
            canShuffle = rankedCache.size > candidates.size
            shuffle()
        }
    }

    /** 换一批：同一批合格候选的层内随机轮换，质量分层不变 */
    fun shuffle() {
        if (rankedCache.isEmpty()) return
        candidates = NamingEngine.sample(rankedCache, limit = 60, seed = Random.nextLong())
        canShuffle = rankedCache.size > candidates.size
    }

    fun rankedSize(): Int = rankedCache.size

    fun evaluate(p: NamingProfile, given: String): ScoredCandidate? = assets.engine.evaluate(p, given)

    fun toggleFavorite(owner: String, scored: ScoredCandidate) {
        scope.launch {
            val exists = db.favoriteDao().countOf(scored.candidate.surname, scored.candidate.given, owner) > 0
            if (exists) {
                db.favoriteDao().delete(scored.candidate.surname, scored.candidate.given, owner)
            } else {
                db.favoriteDao().upsertAll(
                    listOf(
                        FavoriteEntity(
                            owner = owner,
                            surname = scored.candidate.surname,
                            given = scored.candidate.given,
                            failCount = scored.failCount,
                            warnCount = scored.warnCount,
                            checksJson = json.encodeToString<List<CheckResult>>(scored.checks),
                            createdAt = System.currentTimeMillis(),
                            originText = scored.candidate.origin?.text,
                            originBook = scored.candidate.origin?.book,
                            originChapter = scored.candidate.origin?.chapter,
                            originGloss = scored.candidate.origin?.gloss,
                        ),
                    ),
                )
            }
        }
    }

    fun toggleFinalize(favorite: FavoriteEntity) {
        scope.launch { db.favoriteDao().upsertAll(listOf(favorite.copy(finalized = !favorite.finalized))) }
    }

    fun addVote(given: String, voter: String, attitude: String, comment: String) {
        scope.launch {
            db.voteDao().add(
                VoteEntity(
                    given = given, voter = voter.ifBlank { "家人" },
                    attitude = attitude, comment = comment,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun isFavorite(owner: String, surname: String, given: String): Boolean =
        db.favoriteDao().countOf(surname, given, owner) > 0

    /* ---------- 导出 / 导入（.jiaming.json，SAF 分享文件） ---------- */

    fun buildExport(profile: NamingProfile?, favorites: List<FavoriteEntity>, votes: List<VoteEntity> = emptyList()): String {
        val dto = profile?.let {
            ProfileDto(
                surname = it.surname,
                gender = it.gender.name,
                traits = it.traits,
                avoidChars = it.avoidChars.joinToString(""),
                generationChar = it.generationChar,
                generationAtEnd = it.generationAtEnd,
            )
        }
        return json.encodeToString(
            ExportBundle.serializer(),
            ExportBundle(profile = dto, favorites = favorites, votes = votes),
        )
    }

    fun applyImport(text: String, onDone: (Boolean) -> Unit) {
        scope.launch {
            try {
                val bundle = json.decodeFromString(ExportBundle.serializer(), text)
                bundle.profile?.let {
                    db.profileDao().upsert(
                        ProfileEntity(
                            surname = it.surname,
                            gender = it.gender,
                            traitsJson = json.encodeToString(ListSerializer(String.serializer()), it.traits),
                            avoidChars = it.avoidChars,
                            generationChar = it.generationChar,
                            generationAtEnd = it.generationAtEnd,
                        ),
                    )
                }
                if (bundle.favorites.isNotEmpty()) db.favoriteDao().upsertAll(bundle.favorites)
                bundle.votes.forEach { v -> db.voteDao().add(v) }
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }
}
