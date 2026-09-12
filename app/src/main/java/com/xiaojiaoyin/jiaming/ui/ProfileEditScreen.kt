package com.xiaojiaoyin.jiaming.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaojiaoyin.jiaming.domain.model.Gender
import com.xiaojiaoyin.jiaming.domain.model.NamingProfile

private val ALL_TRAITS = listOf(
    "灵动活力", "坚韧大气", "温润平和", "书卷气", "明朗开朗", "独立有主见", "平安顺遂",
)

private data class DialectOption(val id: String, val label: String, val regions: String)

private val DIALECT_OPTIONS = listOf(
    DialectOption("yue", "粤语", "广东/港澳"),
    DialectOption("minnan", "闽南语", "闽南/台湾"),
    DialectOption("chaoshan", "潮汕话", "潮汕"),
    DialectOption("hakka", "客家话", "粤东北/闽西/赣南"),
    DialectOption("wu", "吴语", "上海/苏南/浙北"),
    DialectOption("xinan", "西南官话", "川渝/云贵"),
    DialectOption("zhongyuan", "中原官话", "河南/关中/皖北"),
    DialectOption("xiang", "湘语", "湖南"),
    DialectOption("gan", "赣语", "江西"),
    DialectOption("jin", "晋语", "山西/陕北"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    initial: NamingProfile?,
    onSave: (NamingProfile) -> Unit,
) {
    var surname by remember { mutableStateOf(initial?.surname ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: Gender.UNKNOWN) }
    var traits by remember { mutableStateOf(initial?.traits ?: emptyList()) }
    var avoidInput by remember { mutableStateOf(initial?.avoidChars?.joinToString("") ?: "") }
    var genChar by remember { mutableStateOf(initial?.generationChar ?: "") }
    var genAtEnd by remember { mutableStateOf(initial?.generationAtEnd ?: false) }
    var dialects by remember { mutableStateOf(initial?.dialectIds ?: emptyList()) }

    var hasBirth by remember { mutableStateOf(initial?.birthYear != null) }
    var byear by remember { mutableStateOf(initial?.birthYear?.toString() ?: "") }
    var bmonth by remember { mutableStateOf(initial?.birthMonth?.toString() ?: "") }
    var bday by remember { mutableStateOf(initial?.birthDay?.toString() ?: "") }
    var bhour by remember { mutableStateOf(initial?.birthHour?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("取名档案", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = surname,
            onValueChange = { surname = it.take(2) },
            label = { Text("姓氏（必填）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Column {
            Text("宝宝性别", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Gender.UNKNOWN to "未知/保密",
                    Gender.MALE to "男宝",
                    Gender.FEMALE to "女宝",
                ).forEach { (g, label) ->
                    RadioButton(selected = gender == g, onClick = { gender = g })
                    Text(label, modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .width(84.dp))
                }
            }
        }

        Column {
            Text("期望气质（可多选）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_TRAITS.forEach { t ->
                    FilterChip(
                        selected = t in traits,
                        onClick = { traits = if (t in traits) traits - t else traits + t },
                        label = { Text(t) },
                    )
                }
            }
        }

        Column {
            Text("家乡话扫描（可多选，对应 R10 方言提示）", style = MaterialTheme.typography.titleMedium)
            Text(
                "名字普通话过关后，请家乡长辈用方言再念一遍——雷区表仅供参考，最终以长辈听感为准。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DIALECT_OPTIONS.forEach { d ->
                    FilterChip(
                        selected = d.id in dialects,
                        onClick = { dialects = if (d.id in dialects) dialects - d.id else dialects + d.id },
                        label = { Text(d.label) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = avoidInput,
            onValueChange = { avoidInput = it },
            label = { Text("避讳字（连写，如长辈名用字）") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("命中即淘汰该候选", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
        )

        Column {
            OutlinedTextField(
                value = genChar,
                onValueChange = { genChar = it.take(1) },
                label = { Text("字辈字（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (genChar.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = genAtEnd, onCheckedChange = { genAtEnd = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (genAtEnd) "字辈放在名末" else "字辈放在名首")
                }
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = hasBirth, onCheckedChange = { hasBirth = it })
                Spacer(Modifier.width(8.dp))
                Text("宝宝已出生（解锁八字排盘）", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "未出生时八字排盘不可用：差一个时辰五行就全变，提前算等于编造。出生后填准确时辰再开。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasBirth) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = byear, onValueChange = { byear = it.filter(Char::isDigit).take(4) },
                        label = { Text("年") }, modifier = Modifier.weight(1.2f), singleLine = true,
                    )
                    OutlinedTextField(
                        value = bmonth, onValueChange = { bmonth = it.filter(Char::isDigit).take(2) },
                        label = { Text("月") }, modifier = Modifier.weight(0.9f), singleLine = true,
                    )
                    OutlinedTextField(
                        value = bday, onValueChange = { bday = it.filter(Char::isDigit).take(2) },
                        label = { Text("日") }, modifier = Modifier.weight(0.9f), singleLine = true,
                    )
                    OutlinedTextField(
                        value = bhour, onValueChange = { bhour = it.filter(Char::isDigit).take(2) },
                        label = { Text("时(0-23)") }, modifier = Modifier.weight(1.3f), singleLine = true,
                    )
                }
            }
        }

        Button(
            onClick = {
                onSave(
                    NamingProfile(
                        surname = surname.trim(),
                        gender = gender,
                        traits = traits,
                        avoidChars = avoidInput.map { it.toString() }.distinct(),
                        generationChar = genChar.trim().ifBlank { null },
                        generationAtEnd = genAtEnd,
                        dialectIds = dialects,
                        birthYear = if (hasBirth) byear.toIntOrNull() else null,
                        birthMonth = if (hasBirth) bmonth.toIntOrNull() else null,
                        birthDay = if (hasBirth) bday.toIntOrNull() else null,
                        birthHour = if (hasBirth) bhour.toIntOrNull() else null,
                    ),
                )
            },
            enabled = surname.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存档案") }
        Spacer(Modifier.height(16.dp))
    }
}
