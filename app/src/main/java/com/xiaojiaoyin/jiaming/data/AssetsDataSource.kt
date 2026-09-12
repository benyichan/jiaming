package com.xiaojiaoyin.jiaming.data

import android.content.Context
import com.xiaojiaoyin.jiaming.domain.model.CharLibrary
import com.xiaojiaoyin.jiaming.domain.model.ClassicsLibrary
import com.xiaojiaoyin.jiaming.domain.model.DialectLibrary
import com.xiaojiaoyin.jiaming.domain.model.NegativeLexicon
import com.xiaojiaoyin.jiaming.domain.model.PolyWhitelist
import com.xiaojiaoyin.jiaming.domain.model.SurnamePinyin
import com.xiaojiaoyin.jiaming.domain.model.TraitLexicon
import com.xiaojiaoyin.jiaming.domain.model.TrendData
import com.xiaojiaoyin.jiaming.domain.NamingEngine
import kotlinx.serialization.json.Json

/**
 * assets/data 下 JSON 数据的唯一加载入口。
 * 单元测试通过同构的 TestAssets（直接读文件系统）复用同一份数据，保证测试与生产同源。
 */
class AssetsDataSource(context: Context) {
    private val loader: (String) -> String = { path ->
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    val charLibrary: CharLibrary by lazy { load(loader, "chars.json") }
    val traitLexicon: TraitLexicon by lazy { load(loader, "trait_lexicon.json") }
    val negativeLexicon: NegativeLexicon by lazy { load(loader, "negative_words.json") }
    val polyWhitelist: PolyWhitelist by lazy { load(loader, "poly_whitelist.json") }
    val surnamePinyin: SurnamePinyin by lazy { load(loader, "surnames.json") }
    val classics: ClassicsLibrary by lazy { load(loader, "classics.json") }
    val dialects: DialectLibrary by lazy { load(loader, "dialects.json") }
    val trend: TrendData by lazy { load(loader, "trend.json") }

    val engine: NamingEngine by lazy {
        NamingEngine(
            charLibrary, traitLexicon, negativeLexicon, polyWhitelist, surnamePinyin,
            classics, dialects, trend,
        )
    }

    companion object {
        val jsonFormat = Json { ignoreUnknownKeys = true }

        inline fun <reified T> load(loader: (String) -> String, path: String): T =
            jsonFormat.decodeFromString(loader("data/$path"))
    }
}
