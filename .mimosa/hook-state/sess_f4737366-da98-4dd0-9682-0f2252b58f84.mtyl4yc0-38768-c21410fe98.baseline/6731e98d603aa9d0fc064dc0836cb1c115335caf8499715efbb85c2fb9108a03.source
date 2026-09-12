package com.xiaojiaoyin.jiaming.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xiaojiaoyin.jiaming.domain.model.Gender
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile

@Composable
fun HomeScreen(
    profile: NamingProfile?,
    vm: MainViewModel,
    onEditProfile: () -> Unit,
    onOpenCandidates: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("嘉名", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "透明取名工作台 · 每个候选都给你看得懂的依据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ProfileCard(profile, vm, onEditProfile)

        Button(
            onClick = {
                profile?.let { vm.generate(it) }
                onOpenCandidates()
            },
            enabled = profile != null && profile.surname.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("生成候选名", style = MaterialTheme.typography.titleMedium)
        }

        ManualCheckCard(profile, vm)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileCard(profile: NamingProfile?, vm: MainViewModel, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("取名档案", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(if (profile == null) "去创建" else "编辑") }
            }
            if (profile == null || profile.surname.isBlank()) {
                Text("先填一份取名档案：姓氏、性别期望、想要的气质。", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    vm.saveProfile(
                        NamingProfile(
                            surname = "陈",
                            gender = Gender.UNKNOWN,
                            traits = listOf("灵动活力", "坚韧大气"),
                        ),
                    )
                }) { Text("或者，用示例档案先试试 →") }
            } else {
                val genderText = when (profile.gender) {
                    Gender.MALE -> "男宝"
                    Gender.FEMALE -> "女宝"
                    Gender.UNKNOWN -> "性别保密"
                }
                Text("${profile.surname}家 · $genderText", style = MaterialTheme.typography.titleLarge)
                if (profile.traits.isNotEmpty()) {
                    Text("期望气质：" + profile.traits.joinToString(" / "), style = MaterialTheme.typography.bodyMedium)
                }
                if (profile.avoidChars.isNotEmpty()) {
                    Text("避讳字：" + profile.avoidChars.joinToString("、"), style = MaterialTheme.typography.bodyMedium)
                }
                profile.generationChar?.let {
                    Text(
                        "字辈「$it」（名${if (profile.generationAtEnd) "末" else "首"}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualCheckCard(profile: NamingProfile?, vm: MainViewModel) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("测一个自己想的名字", style = MaterialTheme.typography.titleMedium)
            if (profile == null || profile.surname.isBlank()) {
                Text("需要先创建档案（要有姓氏）。", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(2) },
                        label = { Text("名（1~2 字）") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            val scored = vm.evaluate(profile, input)
                            result = scored?.let {
                                val bad = it.checks.filter { c -> c.level == com.xiaojiaoyin.jiaming.domain.model.CheckLevel.FAIL }
                                val warn = it.checks.filter { c -> c.level == com.xiaojiaoyin.jiaming.domain.model.CheckLevel.WARN }
                                buildString {
                                    append("${profile.surname}$input：")
                                    if (bad.isEmpty() && warn.isEmpty()) append("全部检查通过 ✓")
                                    else {
                                        if (bad.isNotEmpty()) append("硬伤 ${bad.size} 项")
                                        if (bad.isNotEmpty() && warn.isNotEmpty()) append("，")
                                        if (warn.isNotEmpty()) append("注意 ${warn.size} 项")
                                    }
                                }
                            } ?: "「${profile.surname}$input」里有字不在字库，暂时无法体检（字库 v0.1 覆盖取名高频字）"
                        },
                        enabled = input.isNotBlank(),
                    ) { Text("体检") }
                }
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
                }
            }
        }
    }
}
