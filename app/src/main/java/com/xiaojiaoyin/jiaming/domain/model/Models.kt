package com.xiaojiaoyin.jiaming.domain.model

import kotlinx.serialization.Serializable

/**
 * 证据分级（design.md §3）：
 * FACT = 客观可验证；CONVENTION = 文化惯例；CONTESTED = 体系内部分歧。
 * UI 上 CONTESTED 内容必须展示争议说明。
 */
enum class EvidenceGrade { FACT, CONVENTION, CONTESTED }

enum class Gender { MALE, FEMALE, UNKNOWN }

/** 字库单字（assets/data/chars.json）。字段缩写保持 JSON 紧凑：见 docs/GLOSSARY.md */
@Serializable
data class CharData(
    val c: String,          // 汉字
    val y: String,          // 拼音（带调数字，如 xing2）
    val st: Int,            // 笔画数
    val gx: String,         // 结构：du 独体 / lr 左右 / ud 上下 / wrap 包围
    val g: String = "n",    // 性别倾向 m/f/n
    val t: List<String> = emptyList(), // 期望标签
    val poly: Boolean = false,         // 多音字
    val std: Boolean = true,           // 是否在《通用规范汉字表》内
    val nm: Boolean = true,            // 是否推荐用于自由组名（典籍专有字为 false）
)

@Serializable
data class CharLibrary(val version: String, val source: String, val chars: List<CharData>)

@Serializable
data class TraitLexicon(val version: String, val traits: Map<String, List<String>>)

/** 负面词：strong = 强禁忌（脏话/性/疾病/严重不雅）→ FAIL；weak = 一般负面 → WARN */
@Serializable
data class NegativeWord(val w: String, val y: List<String>, val strong: Boolean)

@Serializable
data class NegativeLexicon(val version: String, val source: String, val words: List<NegativeWord>)

@Serializable
data class PolyWhitelist(val version: String, val note: String, val chars: List<String>)

/** 常见姓氏读音表（姓在字库缺失时的兜底，读音以姓氏读法为准，如 任=ren2、单=shan4） */
@Serializable
data class SurnamePinyin(val version: String, val map: Map<String, String>)

/** 典籍条目：原文公版，gloss 自写白话，picks 为人工精选可组名词组 */
@Serializable
data class ClassicEntry(
    val book: String,
    val chapter: String,
    val text: String,
    val gloss: String,
    val picks: List<String> = emptyList(),
)

@Serializable
data class ClassicsLibrary(val version: String, val source: String, val entries: List<ClassicEntry>)

@Serializable
data class DialectRisk(val c: String, val r: String)

@Serializable
data class Dialect(
    val id: String,
    val name: String,
    val regions: String,
    val confidence: String,
    val notes: String,
    val risky: List<DialectRisk> = emptyList(),
)

@Serializable
data class DialectLibrary(val version: String, val source: String, val dialects: List<Dialect>)

@Serializable
data class TrendName(val n: String, val g: String, val year: Int, val region: String, val rank: Int, val src: String)

@Serializable
data class TrendChar(val c: String, val year: Int, val region: String, val src: String)

@Serializable
data class TrendData(val version: String, val source: String, val names: List<TrendName>, val chars: List<TrendChar>)

/** 单音节：base=声韵母，tone=1~4（轻声记 5，比对时按调任意处理） */
data class Syllable(val base: String, val tone: Int)

fun parsePinyin(y: String): Syllable {
    val toneChar = y.last()
    val tone = toneChar.digitToIntOrNull() ?: 5
    return Syllable(y.dropLast(1), tone)
}

/** 声调任意比较用 key："xing2" -> "xing" */
fun String.toneless(): String = parsePinyin(this).base

/** 取名档案（M1 运行时对象；持久化见 data/db） */
data class NamingProfile(
    val surname: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val traits: List<String> = emptyList(),
    val avoidChars: List<String> = emptyList(),   // 避讳字
    val generationChar: String? = null,           // 字辈字
    val generationAtEnd: Boolean = false,         // 字辈在名末（false=名首）
    val dialectIds: List<String> = emptyList(),   // 父母籍贯/定居地方言（R10 扫描）
    // 出生信息（八字排盘用；未出生时为 null，明确不给「假八字」）
    val birthYear: Int? = null,
    val birthMonth: Int? = null,
    val birthDay: Int? = null,
    val birthHour: Int? = null,                   // 0~23，未知为 null
)

/** 典籍出处（M1 预留结构，GeneratorA 于 M2 接入） */
data class Origin(
    val text: String,
    val book: String,
    val chapter: String,
    val gloss: String,
)

data class Candidate(
    val surname: String,
    val given: String,
    val chars: List<CharData>,   // 名字各字的字库数据
    val origin: Origin? = null,
) {
    val full: String get() = surname + given
}

/** SKIP = 该项检查因数据缺失未执行（如姓氏读音未收录），UI 置灰并显示原因 */
enum class CheckLevel { PASS, WARN, FAIL, SKIP }

@Serializable
data class CheckResult(
    val ruleId: String,
    val level: CheckLevel,
    val message: String,
)

data class ScoredCandidate(
    val candidate: Candidate,
    val checks: List<CheckResult>,
    val softScore: Int,   // 仅用于内部排序，UI 不展示为“分数”
) {
    val failCount: Int get() = checks.count { it.level == CheckLevel.FAIL }
    val warnCount: Int get() = checks.count { it.level == CheckLevel.WARN }
}
