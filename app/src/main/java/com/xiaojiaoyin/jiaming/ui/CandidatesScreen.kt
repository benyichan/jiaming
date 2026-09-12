package com.xiaojiaoyin.jiaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojiaoyin.jiaming.domain.model.ScoredCandidate
import com.xiaojiaoyin.jiaming.ui.theme.FailRed
import com.xiaojiaoyin.jiaming.ui.theme.WarnAmber

@Composable
fun CandidatesScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val list = vm.candidates
    val generating = vm.generating

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("候选名", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (generating) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(0.dp))
            } else {
                Text(
                    "${list.size} 个 · 按检查结果排序",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "排序口径：硬伤少 → 注意少 → 气质匹配高。UI 永不打分，依据进面板逐条看。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            items(list, key = { it.candidate.given }) { item ->
                CandidateRow(item) { onOpen(item.candidate.given) }
            }
        }
    }
}

@Composable
private fun CandidateRow(item: ScoredCandidate, onClick: () -> Unit) {
    val fail = item.checks.count { it.level == com.xiaojiaoyin.jiaming.domain.model.CheckLevel.FAIL }
    val warn = item.checks.count { it.level == com.xiaojiaoyin.jiaming.domain.model.CheckLevel.WARN }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.candidate.full,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.candidate.chars.joinToString(" ") { pinyinLabel(it.y) },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fail > 0) Badge("$fail 硬伤", FailRed)
            if (warn > 0) Badge("$warn 注意", WarnAmber)
            if (fail == 0 && warn == 0) Badge("全过", MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Badge(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

fun pinyinLabel(y: String): String {
    val tone = y.last().digitToIntOrNull() ?: 5
    val base = y.dropLast(1)
    // 标注音调用数字后缀（1~4/轻5），比声调符号实现简单且无字体缺字风险
    return if (tone == 5) "$base·轻" else "$base$tone"
}
