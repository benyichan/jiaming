package com.xiaojiaoyin.jiaming.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojiaoyin.jiaming.domain.model.CheckLevel
import com.xiaojiaoyin.jiaming.domain.model.CheckResult
import com.xiaojiaoyin.jiaming.domain.model.Gender
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile
import com.xiaojiaoyin.jiaming.domain.model.ScoredCandidate
import com.xiaojiaoyin.jiaming.domain.model.parsePinyin
import com.xiaojiaoyin.jiaming.ui.theme.FailRed
import com.xiaojiaoyin.jiaming.ui.theme.WarnAmber
import kotlinx.coroutines.launch

/**
 * 证据面板（design.md §9）：名字大字 → 出处卡 → 声调曲线 → 检查清单 → 八字卡（可选模块）。
 * R8 爆款/R10 方言随管线自动出现在清单里。
 */
@Composable
fun EvidencePanelScreen(
    vm: MainViewModel,
    given: String,
    profile: NamingProfile?,
    onBack: () -> Unit,
) {
    val scored: ScoredCandidate? = remember(given, profile) {
        vm.candidates.firstOrNull { it.candidate.given == given }
            ?: profile?.let { vm.evaluate(it, given) }
    }
    val favorites by vm.favoritesFlow.collectAsState(initial = emptyList())
    val baziEnabled by vm.settings.baziEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var owner by remember { mutableStateOf("P1") }
    val shareLayer = rememberShareLayer()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("证据面板", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    runCatching {
                        val f = ShareUtil.exportPng(context, shareLayer, "jiaming-$given.png")
                        ShareUtil.sharePng(context, f, "嘉名 · ${scored?.candidate?.full ?: given}")
                    }
                }
            }) {
                Icon(Icons.Filled.Share, contentDescription = "分享卡片")
            }
        }

        if (scored == null || profile == null) {
            Text(
                "名字体检需要先有档案（含姓氏）。",
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val isFav = favorites.any { it.given == given && it.owner == owner }

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .drawWithContent {
                    shareLayer.record { this@drawWithContent.drawContent() }
                    drawContent()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                scored.candidate.full,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                scored.candidate.chars.joinToString(" ") { pinyinLabel(it.y) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            scored.candidate.origin?.let { o ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("出处 · ${o.book}｜${o.chapter}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("「${o.text}」", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(o.gloss, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "原文为公版典籍；白话释义为项目自写，可点开源仓库校勘。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            ToneCurve(scored)
            CheckList(scored.checks)

            if (baziEnabled) {
                BaziCard(vm, profile)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { scope.launch { vm.toggleFavorite(owner, scored) } }) {
                    Icon(
                        if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFav) "已收藏" else "收藏（$owner）")
                }
                androidx.compose.material3.FilterChip(
                    selected = owner == "P1",
                    onClick = { owner = "P1" },
                    label = { Text("家长一") },
                )
                androidx.compose.material3.FilterChip(
                    selected = owner == "P2",
                    onClick = { owner = "P2" },
                    label = { Text("家长二") },
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** 八字卡（M3 可选模块，默认关）：排盘为确定性历法计算；扶抑倾向为民俗解释，CONTESTED */
@Composable
private fun BaziCard(vm: MainViewModel, profile: NamingProfile) {
    val y = profile.birthYear
    val m = profile.birthMonth
    val d = profile.birthDay
    val h = profile.birthHour
    if (y == null || m == null || d == null) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("八字排盘（民俗参考）", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "档案里还没有出生日期。出生后填准日期与时辰（差一个时辰五行全变），再解锁排盘。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    if (h == null) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("八字排盘（民俗参考）", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "已填日期，但时辰未知。时辰决定时柱，缺时柱的八字是不完整的——去档案里补时辰。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val r = remember(y, m, d, h) { vm.baziEngine.analyze(y, m, d, h) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("八字排盘（民俗参考）", style = MaterialTheme.typography.titleMedium)
            if (r == null) {
                Text("排盘计算失败，请检查日期是否有效。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("四柱：${r.yearPillar}　${r.monthPillar}　${r.dayPillar}　${r.timePillar}　（生肖${r.zodiac}）")
                val wx = r.wuxingCount.entries.joinToString("　") { "${it.key}${it.value}" }
                Text("五行：$wx")
                Text("日主属${r.dayMaster}，整体${r.lean}；扶抑法民俗上宜补${r.suggest}")
                Text(
                    "诚实声明：干支换算是确定的历法计算；「宜补什么」是民俗解释体系（扶抑流派），流派间结论可能不同，无科学结论支持，仅供文化参考。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 声调曲线：三音节调值映射为折线，平仄起伏一眼可读 */
@Composable
private fun ToneCurve(scored: ScoredCandidate) {
    val tones = scored.candidate.chars.map { parsePinyin(it.y).tone }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("声调曲线", style = MaterialTheme.typography.titleMedium)
            Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                val pad = 24f
                val stepX = (size.width - pad * 2) / (tones.size - 1).coerceAtLeast(1)
                val toneY = { t: Int ->
                    val h = when (t) { 1 -> 0.15f; 2 -> 0.4f; 3 -> 0.65f; else -> 0.9f }
                    size.height * h
                }
                for (i in 0..3) {
                    val y = size.height * (0.15f + i * 0.25f)
                    drawLine(
                        androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.15f),
                        Offset(pad, y), Offset(size.width - pad, y), 1f,
                    )
                }
                val path = Path()
                tones.forEachIndexed { i, t ->
                    val x = pad + stepX * i
                    if (i == 0) path.moveTo(x, toneY(t)) else path.lineTo(x, toneY(t))
                }
                drawPath(path, androidx.compose.ui.graphics.Color(0xFF2E9E76), style = Stroke(4f, cap = StrokeCap.Round))
                tones.forEachIndexed { i, t ->
                    drawCircle(androidx.compose.ui.graphics.Color(0xFF1F7A5B), 6f, Offset(pad + stepX * i, toneY(t)))
                }
            }
            Text(
                "读感提示：末字" + if (tones.last() in listOf(2, 4)) "响亮收尾" else "偏柔收尾",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CheckList(checks: List<CheckResult>) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("检查清单（R1~R10，全部依据见 docs/GLOSSARY.md）", style = MaterialTheme.typography.titleMedium)
            checks.forEach { c ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (c.level) {
                        CheckLevel.PASS -> Icon(
                            Icons.Filled.Check, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        CheckLevel.WARN -> Icon(
                            Icons.Filled.WarningAmber, null, Modifier.size(20.dp), tint = WarnAmber,
                        )
                        CheckLevel.FAIL -> Icon(
                            Icons.Filled.Close, null, Modifier.size(20.dp), tint = FailRed,
                        )
                        CheckLevel.SKIP -> Text("—", color = MaterialTheme.colorScheme.outline)
                    }
                    Column {
                        Text(ruleTitle(c.ruleId), style = MaterialTheme.typography.titleMedium)
                        Text(
                            c.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (c.level == CheckLevel.SKIP) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun ruleTitle(id: String): String = when (id) {
    "R1" -> "R1 规范汉字（上户口红线）"
    "R2" -> "R2 避讳字"
    "R3" -> "R3 字辈"
    "R4" -> "R4 多音字"
    "R5" -> "R5 声调配平"
    "R6" -> "R6 字形搭配"
    "R7" -> "R7 谐音扫描"
    "R8" -> "R8 爆款降权"
    "R9" -> "R9 姓氏连读"
    "R10" -> "R10 家乡话提示"
    else -> id
}
