package com.xiaojiaoyin.jiaming

import com.xiaojiaoyin.jiaming.domain.NamingEngine
import com.xiaojiaoyin.jiaming.domain.model.Candidate
import com.xiaojiaoyin.jiaming.domain.model.CharData
import com.xiaojiaoyin.jiaming.domain.model.CheckLevel
import com.xiaojiaoyin.jiaming.domain.model.Gender
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import com.xiaojiaoyin.jiaming.domain.model.NegativeLexicon
import com.xiaojiaoyin.jiaming.domain.model.PolyWhitelist
import com.xiaojiaoyin.jiaming.domain.model.SurnamePinyin
import com.xiaojiaoyin.jiaming.domain.model.TraitLexicon
import com.xiaojiaoyin.jiaming.domain.model.TrendData
import com.xiaojiaoyin.jiaming.domain.rule.RuleContext
import com.xiaojiaoyin.jiaming.domain.rule.RulePipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 黄金用例（design.md §11）：规则正反例 + 端到端生成。
 * 数据与生产同源：直接读 app/src/main/assets/data 目录下的 json（JUnit 工作目录 = app 模块目录）。
 */
class GoldenCasesTest {

    private lateinit var engine: NamingEngine
    private lateinit var charByChar: Map<Char, CharData>
    private lateinit var deps: Deps

    data class Deps(
        val negative: NegativeLexicon,
        val whitelist: PolyWhitelist,
        val surnames: SurnamePinyin,
        val trend: TrendData,
    )

    @Before
    fun setUp() {
        val lib = TestAssets.charLibrary()
        val traits = TestAssets.traitLexicon()
        val negative = TestAssets.negativeLexicon()
        val whitelist = TestAssets.polyWhitelist()
        val surnames = TestAssets.surnames()
        val classics = TestAssets.classics()
        val dialects = TestAssets.dialects()
        val trend = TestAssets.trend()
        engine = NamingEngine(lib, traits, negative, whitelist, surnames, classics, dialects, trend)
        charByChar = lib.chars.associateBy { it.c.first() }
        deps = Deps(negative, whitelist, surnames, trend)
    }

    private fun profile(
        surname: String = "陈",
        gender: Gender = Gender.UNKNOWN,
        traits: List<String> = listOf("灵动活力", "坚韧大气"),
        dialects: List<String> = emptyList(),
    ) = NamingProfile(surname = surname, gender = gender, traits = traits, dialectIds = dialects)

    private fun ctx(p: NamingProfile = profile()) = RuleContext(
        profile = p,
        charIndex = charByChar,
        polyWhitelist = deps.whitelist.chars.map { it.first() }.toSet(),
        negativeLexicon = deps.negative,
        surnamePinyin = deps.surnames,
        dialectLibrary = TestAssets.dialects(),
        trendData = deps.trend,
    )

    private fun runChecks(surname: String, given: String, p: NamingProfile = profile(surname)): Map<String, CheckLevel> {
        val chars = given.map { c -> charByChar[c] ?: CharData(c.toString(), "?", 1, "du") }
        val cand = Candidate(surname, given, chars)
        return RulePipeline.default().run(cand, ctx(p)).associate { it.ruleId to it.level }
    }

    /* R1：规范汉字表 */
    @Test
    fun r1_nonStandardCharFails() {
        val fake = CharData("頔", "di2", 9, "lr", std = false)
        val cand = Candidate("陈", "頔轩", listOf(fake, charByChar.getValue('轩')))
        val r = RulePipeline.default().run(cand, ctx()).first { it.ruleId == "R1" }
        assertEquals(CheckLevel.FAIL, r.level)
    }

    /* R4：多音字白名单内外 */
    @Test
    fun r4_polyphonicOutsideWhitelistWarns() {
        assertEquals(CheckLevel.WARN, runChecks("陈", "柏川")["R4"])
    }

    @Test
    fun r4_xingInWhitelistPasses() {
        assertEquals(CheckLevel.PASS, runChecks("陈", "景行")["R4"])
    }

    /* R7：谐音扫描 */
    @Test
    fun r7_duZiTengFails() {
        assertEquals(CheckLevel.FAIL, runChecks("杜", "子腾")["R7"])
    }

    @Test
    fun r7_yangWeiFails() {
        assertEquals(CheckLevel.FAIL, runChecks("杨", "伟")["R7"])
    }

    @Test
    fun r7_siNearSiWarns() {
        // 思(sī) 与 死(sǐ) 声韵全同仅调异；「死」为一般负面词 → WARN
        assertEquals(CheckLevel.WARN, runChecks("陈", "思远")["R7"])
    }

    @Test
    fun r7_cleanNamePasses() {
        assertEquals(CheckLevel.PASS, runChecks("陈", "清越")["R7"])
    }

    /* R9：姓氏连读（雷词前两音节） */
    @Test
    fun r9_shiZhenXiangFails() {
        assertEquals(CheckLevel.FAIL, runChecks("史", "珍香")["R9"])
    }

    /* R8：爆款降权（官方公开数据） */
    @Test
    fun r8_trendNameWarns() {
        // 梓涵 = 2020 全国女名 Top5
        assertEquals(CheckLevel.WARN, runChecks("陈", "梓涵")["R8"])
    }

    @Test
    fun r8_hotCharWarns() {
        // 辰 为 2020 全国用字前五
        assertEquals(CheckLevel.WARN, runChecks("陈", "宇辰")["R8"])
    }

    @Test
    fun r8_cleanNamePasses() {
        assertEquals(CheckLevel.PASS, runChecks("陈", "清越")["R8"])
    }

    /* R10：方言提示 */
    @Test
    fun r10_yueRiskyCharWarns() {
        // 婷 在粤语雷区表；未选方言时 SKIP
        assertEquals(CheckLevel.SKIP, runChecks("陈", "诗婷")["R10"])
        val levels = runChecks("陈", "诗婷", profile(dialects = listOf("yue")))
        assertEquals(CheckLevel.WARN, levels["R10"])
    }

    /* GeneratorA：典籍出处 */
    @Test
    fun classics_generateWithOrigin() {
        val out = engine.generate(profile(), limit = 200)
        val withOrigin = out.filter { it.candidate.origin != null }
        assertTrue("典籍候选不应为空", withOrigin.isNotEmpty())
        assertTrue(
            "典籍候选应覆盖多种典籍，实际出处：${withOrigin.mapNotNull { it.candidate.origin?.book }.distinct()}",
            withOrigin.mapNotNull { it.candidate.origin?.book }.distinct().size >= 3,
        )
    }

    /* 端到端：候选量与性能（验收口径：30 秒内 ≥20 候选） */
    @Test
    fun generate_producesEnoughCandidatesFast() {
        val start = System.currentTimeMillis()
        val out = engine.generate(profile(), limit = 60)
        val elapsed = System.currentTimeMillis() - start
        assertTrue("实际耗时 ${elapsed}ms", elapsed < 30_000)
        assertTrue("候选仅 ${out.size} 个", out.size >= 20)
        assertEquals(0, out.first().failCount)
    }

    /* 测名入口：字库外字返回 null（诚实提示，而非假结果） */
    @Test
    fun evaluate_unknownCharReturnsNull() {
        org.junit.Assert.assertNull(engine.evaluate(profile(), "頔轩"))
    }
}
