package com.xiaojiaoyin.jiaming.domain.rule

import com.xiaojiaoyin.jiaming.domain.model.Candidate
import com.xiaojiaoyin.jiaming.domain.model.CharData
import com.xiaojiaoyin.jiaming.domain.model.CheckLevel
import com.xiaojiaoyin.jiaming.domain.model.CheckResult
import com.xiaojiaoyin.jiaming.domain.model.DialectLibrary
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import com.xiaojiaoyin.jiaming.domain.model.NegativeLexicon
import com.xiaojiaoyin.jiaming.domain.model.PolyWhitelist
import com.xiaojiaoyin.jiaming.domain.model.SurnamePinyin
import com.xiaojiaoyin.jiaming.domain.model.TrendData
import com.xiaojiaoyin.jiaming.domain.model.parsePinyin

/**
 * 规则执行上下文：只读数据，跨规则共享。
 * 字库索引、多音字白名单、负面词库、姓氏读音表全部来自 assets 数据文件，带版本与来源。
 */
class RuleContext(
    val profile: NamingProfile,
    charIndex: Map<Char, CharData>,
    val polyWhitelist: Set<Char>,
    val negativeLexicon: NegativeLexicon,
    val surnamePinyin: SurnamePinyin,
    val dialectLibrary: DialectLibrary? = null,
    val trendData: TrendData? = null,
) {
    val charIndex = charIndex

    /** 负面词按首音节索引（R7 性能优化：避免每次全表扫描） */
    val negativeByHead: Map<String, List<Pair<List<String>, Boolean>>> =
        negativeLexicon.words
            .filter { it.y.isNotEmpty() }
            .groupBy({ parsePinyin(it.y.first()).base }) { it.y to it.strong }

    /** 姓氏读音：字库优先（字库未收姓氏字时走姓氏表）；返回 null = 未收录 */
    fun surnamePinyinOf(surname: String): String? {
        if (surname.isEmpty()) return null
        charIndex[surname.first()]?.let { return it.y }
        return surnamePinyin.map[surname]
    }
}

interface NamingRule {
    val id: String
    fun apply(candidate: Candidate, ctx: RuleContext): CheckResult
}

/* ------------------------------------------------- */
/* R1 规范汉字表：上户口硬标准（FACT，来源：国务院 2013 公开通用规范汉字表） */
class StandardTableRule : NamingRule {
    override val id = "R1"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val bad = c.chars.filter { !it.std }
        return if (bad.isEmpty()) {
            CheckResult(id, CheckLevel.PASS, "全部用字在《通用规范汉字表》内，可正常登记户口")
        } else {
            CheckResult(id, CheckLevel.FAIL, "「${bad.joinToString("、") { it.c }}」不在《通用规范汉字表》内，可能无法上户口")
        }
    }
}

/* R2 避讳字（CONVENTION：汉族避长辈名讳传统；字表由用户输入） */
class AvoidCharsRule : NamingRule {
    override val id = "R2"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val avoid = ctx.profile.avoidChars.toSet()
        val hit = c.chars.map { it.c }.filter { it in avoid } +
            (if (c.surname in avoid) listOf(c.surname) else emptyList())
        return if (hit.isEmpty()) {
            CheckResult(id, CheckLevel.PASS, "未命中避讳字")
        } else {
            CheckResult(id, CheckLevel.FAIL, "命中避讳字：${hit.joinToString("、")}")
        }
    }
}

/* R3 字辈约束（CONVENTION：家族字辈由用户输入；位置默认名首，可设名末） */
class GenerationCharRule : NamingRule {
    override val id = "R3"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val gen = ctx.profile.generationChar ?: return CheckResult(id, CheckLevel.PASS, "未启用字辈")
        if (c.given.isEmpty()) return CheckResult(id, CheckLevel.FAIL, "名字为空")
        val actual = if (ctx.profile.generationAtEnd) c.given.last() else c.given.first()
        return if (actual.toString() == gen) {
            CheckResult(id, CheckLevel.PASS, "字辈「$gen」位置正确")
        } else {
            CheckResult(id, CheckLevel.FAIL, "字辈应为「$gen」（名${if (ctx.profile.generationAtEnd) "末" else "首"}），实际为「$actual」")
        }
    }
}

/* R4 多音字（FACT：字库标注；白名单 = 人名中几乎只读一个音的字） */
class PolyphonicRule : NamingRule {
    override val id = "R4"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val risky = c.chars.filter { it.poly && it.c.first() !in ctx.polyWhitelist }
        return if (risky.isEmpty()) {
            CheckResult(id, CheckLevel.PASS, "无易误读多音字（白名单内单音字不计）")
        } else {
            CheckResult(id, CheckLevel.WARN, "多音字：${risky.joinToString("、") { it.c }}，可能被叫错")
        }
    }
}

/* R5 声调配平（CONVENTION：音律通则——三连同调拗口、末字平收/仄收更响亮） */
class ToneRule : NamingRule {
    override val id = "R5"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val sPinyin = ctx.surnamePinyinOf(c.surname)
            ?: return CheckResult(id, CheckLevel.SKIP, "姓氏「${c.surname}」读音未收录，音律检查未执行")
        val tones = listOf(parsePinyin(sPinyin).tone) + c.chars.map { parsePinyin(it.y).tone }
        val allSame = tones.distinct().size == 1
        return if (allSame) {
            CheckResult(id, CheckLevel.WARN, "三字声调全同，连读偏平，建议错开")
        } else {
            CheckResult(id, CheckLevel.PASS, "声调有起伏，读感尚可")
        }
    }

    companion object {
        /** 音律软分：末字阳平/去声收尾更响 +2；平仄交替每处 +1；三连同调 -2 */
        fun softScore(c: Candidate, ctx: RuleContext): Int {
            val sPinyin = ctx.surnamePinyinOf(c.surname) ?: return 0
            val tones = listOf(parsePinyin(sPinyin).tone) + c.chars.map { parsePinyin(it.y).tone }
            var score = 0
            if (tones.last() == 2 || tones.last() == 4) score += 2
            val toneClass = tones.map { if (it <= 2) 0 else 1 } // 平0 仄1
            score += (1 until toneClass.size).count { toneClass[it] != toneClass[it - 1] }
            if (tones.distinct().size == 1) score -= 2
            return score
        }
    }
}

/* R6 字形搭配（CONVENTION：书写美学——笔画悬殊难写、结构雷同呆板） */
class GlyphRule : NamingRule {
    override val id = "R6"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val sData = ctx.charIndex[c.surname.first()]
        val issues = mutableListOf<String>()
        val strokes = mutableListOf<Int>()
        val structs = mutableListOf<String>()
        sData?.let { strokes.add(it.st); structs.add(it.gx) }
        c.chars.forEach { strokes.add(it.st); structs.add(it.gx) }
        for (i in 1 until strokes.size) {
            if (kotlin.math.abs(strokes[i] - strokes[i - 1]) >= 10) {
                issues.add("相邻字笔画差过大（${strokes[i - 1]}画与${strokes[i]}画）")
                break
            }
        }
        if (structs.size == 3 && structs.distinct().size == 1) {
            issues.add("三字结构雷同，书写单调")
        }
        return if (issues.isEmpty()) {
            CheckResult(id, CheckLevel.PASS, "字形搭配协调")
        } else {
            CheckResult(id, CheckLevel.WARN, issues.joinToString("；"))
        }
    }
}

/**
 * R7 谐音扫描（FACT：拼音比对口径——声韵母全同、声调任意即判同音；
 * 强禁忌词 FAIL，一般负面词 WARN。口径细节见 docs/GLOSSARY.md。
 * 实现走首音节索引，避免对每个候选全表扫描。）
 */
class HomophoneRule : NamingRule {
    override val id = "R7"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val sPinyin = ctx.surnamePinyinOf(c.surname)
            ?: return CheckResult(id, CheckLevel.SKIP, "姓氏「${c.surname}」读音未收录，谐音检查未执行")
        val full = listOf(parsePinyin(sPinyin)) + c.chars.map { parsePinyin(it.y) }
        val fullBases = full.map { it.base }
        val hits = mutableListOf<String>()
        var strong = false
        var weak = false
        for (start in fullBases.indices) {
            for ((wordY, isStrong) in ctx.negativeByHead[fullBases[start]].orEmpty()) {
                if (start + wordY.size > fullBases.size) continue
                val matched = wordY.indices.all { k -> fullBases[start + k] == parsePinyin(wordY[k]).base }
                if (matched) {
                    val word = ctx.negativeLexicon.words.first { it.y == wordY && it.strong == isStrong }
                    if (word.w !in hits) hits.add(word.w)
                    if (isStrong) strong = true else weak = true
                }
            }
        }
        return when {
            strong -> CheckResult(id, CheckLevel.FAIL, "与不雅词同音：「${hits.joinToString("、")}」")
            weak -> CheckResult(id, CheckLevel.WARN, "与负面词同音（一般忌讳）：${hits.joinToString("、")}")
            else -> CheckResult(id, CheckLevel.PASS, "普通话谐音检查通过")
        }
    }
}

/**
 * R9 姓氏连读特检：姓+名首字组成两音节，与两字负面词比对。
 * 与 R7 拆开的理由：部分雷点只在「姓连名首字」成词（史+珍 → 屎真），便于独立测试与社区讨论。
 */
class SurnamePrefixRule : NamingRule {
    override val id = "R9"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        if (c.given.isEmpty()) return CheckResult(id, CheckLevel.SKIP, "名字为空")
        val sPinyin = ctx.surnamePinyinOf(c.surname)
            ?: return CheckResult(id, CheckLevel.SKIP, "姓氏「${c.surname}」读音未收录，连读检查未执行")
        val prefix = listOf(parsePinyin(sPinyin), parsePinyin(c.chars.first().y))
        val hits = mutableListOf<String>()
        var strong = false
        var weak = false
        for (word in ctx.negativeLexicon.words) {
            // 取雷词前两音节比对：连读踩雷不看第三字（史+珍 → 屎真[香]）
            val wSyl = word.y.map { parsePinyin(it) }.take(2)
            if (wSyl.size < 2) continue
            if (wSyl.indices.all { k -> prefix[k].base == wSyl[k].base }) {
                hits.add(word.w)
                if (word.strong) strong = true else weak = true
            }
        }
        return when {
            strong -> CheckResult(id, CheckLevel.FAIL, "姓氏连读成不雅词：「${hits.joinToString("、")}」")
            weak -> CheckResult(id, CheckLevel.WARN, "姓氏连读近似负面词：${hits.joinToString("、")}")
            else -> CheckResult(id, CheckLevel.PASS, "姓氏连读检查通过")
        }
    }
}

/**
 * R8 爆款降权（FACT：全部依据官方公开姓名报告；数据带年份与出处，提示给用户而非悄悄淘汰）
 */
class TrendRule : NamingRule {
    override val id = "R8"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val trend = ctx.trendData
            ?: return CheckResult(id, CheckLevel.SKIP, "爆款数据未加载")
        val full = c.surname + c.given
        val issues = mutableListOf<String>()
        trend.names.firstOrNull { it.n == c.given }?.let {
            issues.add("与 ${it.year} 年${it.region}新生儿热门名 Top${it.rank}「${it.n}」撞名（${it.src}）")
        }
        c.chars.map { it.c }.distinct().forEach { ch ->
            trend.chars.firstOrNull { it.c == ch }?.let {
                issues.add("用字「$ch」为 ${it.year} 年${it.region}新生儿爆款字（${it.src}）")
            }
        }
        return when {
            issues.isEmpty() -> CheckResult(id, CheckLevel.PASS, "未命中近年官方爆款名/字")
            else -> CheckResult(id, CheckLevel.WARN, issues.take(2).joinToString("；"))
        }
    }
}

/**
 * R10 方言提示（CONTESTED：雷区表移植自开源 skill，置信度按方言标注；
 * 不做精确拟音——凡是命中风险字，一律给「请长辈念一遍」的求证建议）
 */
class DialectRule : NamingRule {
    override val id = "R10"
    override fun apply(c: Candidate, ctx: RuleContext): CheckResult {
        val lib = ctx.dialectLibrary
            ?: return CheckResult(id, CheckLevel.SKIP, "方言库未加载")
        if (ctx.profile.dialectIds.isEmpty()) {
            return CheckResult(id, CheckLevel.SKIP, "未选择籍贯方言；建议至少选一个，或请家人用家乡话各念一遍")
        }
        val nameChars = c.chars.map { it.c }.toSet()
        val issues = mutableListOf<String>()
        for (dialect in lib.dialects.filter { it.id in ctx.profile.dialectIds }) {
            val hits = dialect.risky.filter { it.c in nameChars }
            if (hits.isNotEmpty()) {
                issues.add("【${dialect.name}】${hits.joinToString("；") { "${it.c}：${it.r}" }}（置信度${dialect.confidence}，请${dialect.name}长辈念一遍）")
            }
        }
        return when {
            issues.isEmpty() -> CheckResult(id, CheckLevel.PASS, "所选方言的已知雷区未命中")
            else -> CheckResult(id, CheckLevel.WARN, issues.take(2).joinToString("\n"))
        }
    }
}

/** 管线：按固定顺序全部执行（FAIL 不中断——用户需要看到全部雷点） */
class RulePipeline(rules: List<NamingRule>) {
    private val ordered = rules

    fun run(c: Candidate, ctx: RuleContext): List<CheckResult> = ordered.map { it.apply(c, ctx) }

    companion object {
        fun default(): RulePipeline = RulePipeline(
            listOf(
                StandardTableRule(),
                AvoidCharsRule(),
                GenerationCharRule(),
                PolyphonicRule(),
                ToneRule(),
                GlyphRule(),
                HomophoneRule(),
                SurnamePrefixRule(),
                TrendRule(),
                DialectRule(),
            ),
        )
    }
}
