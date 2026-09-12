package com.xiaojiaoyin.jiaming.domain

import com.xiaojiaoyin.jiaming.domain.generate.ClassicsGenerator
import com.xiaojiaoyin.jiaming.domain.generate.ComboGenerator
import com.xiaojiaoyin.jiaming.domain.generate.TraitGenerator
import com.xiaojiaoyin.jiaming.domain.model.Candidate
import com.xiaojiaoyin.jiaming.domain.model.CharData
import com.xiaojiaoyin.jiaming.domain.model.CharLibrary
import com.xiaojiaoyin.jiaming.domain.model.ClassicsLibrary
import com.xiaojiaoyin.jiaming.domain.model.DialectLibrary
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import com.xiaojiaoyin.jiaming.domain.model.NegativeLexicon
import com.xiaojiaoyin.jiaming.domain.model.PolyWhitelist
import com.xiaojiaoyin.jiaming.domain.model.ScoredCandidate
import com.xiaojiaoyin.jiaming.domain.model.SurnamePinyin
import com.xiaojiaoyin.jiaming.domain.model.TraitLexicon
import com.xiaojiaoyin.jiaming.domain.model.TrendData
import com.xiaojiaoyin.jiaming.domain.rule.RuleContext
import com.xiaojiaoyin.jiaming.domain.rule.RulePipeline
import com.xiaojiaoyin.jiaming.domain.rule.ToneRule

/**
 * 取名总引擎：生成（A/B/C 三路）→ 规则管线 → 排序截断。
 * 排序口径：FAIL 少 → WARN 少 → 软分高。软分只在内部使用，UI 永不显示为“分数”（design.md §4.2）。
 */
class NamingEngine(
    library: CharLibrary,
    private val traitLexicon: TraitLexicon,
    private val negativeLexicon: NegativeLexicon,
    private val polyWhitelist: PolyWhitelist,
    private val surnamePinyin: SurnamePinyin,
    private val classics: ClassicsLibrary,
    private val dialectLibrary: DialectLibrary,
    private val trendData: TrendData,
) {
    private val charByChar: Map<Char, CharData> = library.chars.associateBy { it.c.first() }

    private val traitGen = TraitGenerator(traitLexicon).apply { charByChar = this@NamingEngine.charByChar }
    private val classicsGen = ClassicsGenerator(classics, charByChar)
    private val comboGen = ComboGenerator(library)
    private val pipeline = RulePipeline.default()

    private fun context(profile: NamingProfile) = RuleContext(
        profile = profile,
        charIndex = charByChar,
        polyWhitelist = polyWhitelist.chars.map { it.first() }.toSet(),
        negativeLexicon = negativeLexicon,
        surnamePinyin = surnamePinyin,
        dialectLibrary = dialectLibrary,
        trendData = trendData,
    )

    fun generate(profile: NamingProfile, limit: Int = 60): List<ScoredCandidate> {
        if (profile.surname.isBlank()) return emptyList()
        val ctx = context(profile)
        val raw = LinkedHashMap<String, Candidate>()
        // A 典籍（带出处，软分+2）→ B 标签（气质对齐）→ C 字库（兜底多样性）
        classicsGen.generate(profile).forEach { raw.putIfAbsent(it.given, it) }
        traitGen.generate(profile).forEach { raw.putIfAbsent(it.given, it) }
        comboGen.generate(profile).forEach { raw.putIfAbsent(it.given, it) }

        val order = compareBy<ScoredCandidate>({ it.failCount }, { it.warnCount }, { -it.softScore })
        val all = raw.values.map { cand ->
            val checks = pipeline.run(cand, ctx)
            ScoredCandidate(cand, checks, softScore(cand, ctx))
        }
        return all.sortedWith(order).take(limit)
    }

    /** 单名体检：「测名」入口——用户自拟名字跑同一套管线 */
    fun evaluate(profile: NamingProfile, given: String): ScoredCandidate? {
        if (given.isEmpty() || given.length !in 1..2) return null
        val ctx = context(profile)
        val chars = given.map { c -> charByChar[c] ?: return null }
        val cand = Candidate(profile.surname, given, chars)
        val checks = pipeline.run(cand, ctx)
        return ScoredCandidate(cand, checks, softScore(cand, ctx))
    }

    private fun softScore(c: Candidate, ctx: RuleContext): Int {
        var s = ToneRule.softScore(c, ctx)
        val covered = c.chars.flatMap { it.t }.toSet()
        s += covered.intersect(ctx.profile.traits.toSet()).size * 3
        // 典籍出处：与「双标签命中」同级的文化权重，让有出处的候选能自然进入前列
        if (c.origin != null) s += 6
        return s
    }
}
