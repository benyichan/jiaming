package com.xiaojiaoyin.jiaming.domain.bazi

/**
 * 八字排盘（确定性历法计算，FACT 层）+ 五行统计与扶抑倾向（CONTESTED：民俗解释体系）。
 *
 * 诚实声明（同时展示在 UI）：
 * - 干支纪年月日时是确定的历法换算，由 lunar 库完成；
 * - 从八字推出「喜用神」存在扶抑/调候/格局等流派分歧，本实现采用最简的扶抑计数法，
 *   结论仅供民俗文化参考，无科学结论支持。
 */
class BaziEngine {

    data class Pillar(val ganZhi: String)

    data class Result(
        val yearPillar: String,
        val monthPillar: String,
        val dayPillar: String,
        val timePillar: String,
        val wuxingCount: Map<String, Int>, // 木火土金水 → 字数（天干全算，地支按本气）
        val dayMaster: String,             // 日主五行
        val lean: String,                  // 偏强 / 偏弱 / 平衡
        val suggest: String,               // 民俗扶抑法宜补的五行
        val zodiac: String,                // 生肖
    )

    private val ganWx = mapOf(
        "甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火",
        "戊" to "土", "己" to "土", "庚" to "金", "辛" to "金",
        "壬" to "水", "癸" to "水",
    )

    // 地支按本气五行
    private val zhiWx = mapOf(
        "寅" to "木", "卯" to "木", "巳" to "火", "午" to "火",
        "申" to "金", "酉" to "金", "亥" to "水", "子" to "水",
        "辰" to "土", "戌" to "土", "丑" to "土", "未" to "土",
    )

    private val sheng = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木") // 我生（泄）
    private val ke = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")   // 我克
    private val shengMe = mapOf("木" to "水", "火" to "木", "土" to "火", "金" to "土", "水" to "金") // 生我（印）
    private val keMe = mapOf("木" to "金", "火" to "水", "土" to "木", "金" to "火", "水" to "土")   // 克我（官杀）

    fun analyze(year: Int, month: Int, day: Int, hour: Int): Result? = try {
        val solar = com.nlf.calendar.Solar.fromYmdHms(year, month, day, hour, 0, 0)
        val lunar = solar.lunar
        val ec = lunar.eightChar
        val y = ec.year
        val m = ec.month
        val d = ec.day
        val t = ec.time

        val all = y + m + d + t
        val count = mutableMapOf("木" to 0, "火" to 0, "土" to 0, "金" to 0, "水" to 0)
        all.forEach { ch ->
            ganWx[ch.toString()]?.let { count.merge(it, 1, Int::plus) }
            zhiWx[ch.toString()]?.let { count.merge(it, 1, Int::plus) }
        }
        val dayGan = d.first().toString()
        val dayMaster = ganWx[dayGan] ?: return null

        // 扶抑计数法（CONTESTED）：同党 = 生我 + 同我；异党 = 我生 + 我克 + 克我
        val same = (count[shengMe[dayMaster]] ?: 0) + (count[dayMaster] ?: 0)
        val diff = (count[sheng[dayMaster]] ?: 0) + (count[ke[dayMaster]] ?: 0) + (count[keMe[dayMaster]] ?: 0)
        val (lean, suggest) = when {
            same > diff + 1 -> "偏强" to "${sheng[dayMaster]}或${ke[dayMaster]}（泄与克）"
            diff > same + 1 -> "偏弱" to "${shengMe[dayMaster]}或$dayMaster（生扶）"
            else -> "平衡" to "$dayMaster 或 ${shengMe[dayMaster]}"
        }

        Result(
            yearPillar = y, monthPillar = m, dayPillar = d, timePillar = t,
            wuxingCount = count, dayMaster = dayMaster, lean = lean, suggest = suggest,
            zodiac = lunar.yearShengXiao,
        )
    } catch (e: Exception) {
        null
    }
}
