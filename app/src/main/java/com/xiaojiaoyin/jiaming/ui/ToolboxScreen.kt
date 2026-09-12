package com.xiaojiaoyin.jiaming.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 工具箱（M3）：官方查重名聚合 + 八字模块开关 + 数据来源 + 免责 + 小脚印联动。
 */
@Composable
fun ToolboxScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val baziEnabled by vm.settings.baziEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("工具箱", style = MaterialTheme.typography.headlineSmall)

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("官方查重名（跳转官方渠道）", style = MaterialTheme.typography.titleMedium)
                Text(
                    "本应用不联网、不自造重名数据。以下为官方入口，查询结果以官方为准。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "· 公安部「互联网+政务服务平台」同名查询（全国）\n· 各省公安「一网通办」App 内搜「重名查询」\n· 出生登记前，也可到户籍窗口现场咨询",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "打开 zwfw.mps.gov.cn →",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zwfw.mps.gov.cn")))
                        }
                    },
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("八字排盘（民俗参考）", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "默认关闭。开启后在证据面板显示四柱与五行；排盘是历法计算，「宜补什么」是民俗解释，无科学结论。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = baziEnabled, onCheckedChange = { v -> scope.launch { vm.settings.setBazi(v) } })
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("数据与依据一览", style = MaterialTheme.typography.titleMedium)
                Text(
                    "字库与规则：项目精校，见仓库 docs/DATA_SOURCES.md\n典籍出处：《诗经》《楚辞》《论语》《周易》公版原文\n爆款字：公安部及各地公安公开姓名报告\n方言提示：开源 skill 移植，CONTESTED 标注\n负面词库：项目自建，社区可持续增补",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("小脚印联动", style = MaterialTheme.typography.titleMedium)
                Text(
                    "定名后想直接开成长档案？装了「小脚印」的话可以一键跳转。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "打开小脚印 →",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                context.packageManager.getLaunchIntentForPackage("com.xiaojiaoyin.baby"),
                            )
                        }
                    },
                )
            }
        }

        Text(
            "嘉名是一款开源离线应用，不联网、无广告、不上传任何数据。八字与方言内容为传统民俗文化参考，请勿作为决策依据。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}
