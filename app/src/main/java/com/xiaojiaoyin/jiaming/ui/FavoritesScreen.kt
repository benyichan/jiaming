package com.xiaojiaoyin.jiaming.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojiaoyin.jiaming.data.db.FavoriteEntity
import com.xiaojiaoyin.jiaming.data.db.VoteEntity
import com.xiaojiaoyin.jiaming.domain.model.CheckResult
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * 收藏页：P1/P2 双列 + 共同喜欢 + 家人意见登记 + 定名 + 导出/导入。
 * 纯本地协作（design.md §8）：导出 .jiaming.json 经 SAF 分享，对方导入合并。
 */
@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val all by vm.favoritesFlow.collectAsState(initial = emptyList())
    val votes by vm.votesFlow.collectAsState(initial = emptyList())
    val profile by vm.profileFlow.collectAsState(initial = null)

    var tab by remember { mutableIntStateOf(0) } // 0=共同喜欢 1=家长一 2=家长二
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var voteTarget by remember { mutableStateOf<FavoriteEntity?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = pendingExport
        if (uri != null && text != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入文件")
                }.onSuccess { snackbar.showSnackbar("已导出（含家人意见，导入方合并后可见共同喜欢）") }
                    .onFailure { snackbar.showSnackbar("导出失败：${it.message}") }
            }
        }
        pendingExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("无法读取文件")
                }.onSuccess { text ->
                    vm.applyImport(text) { ok ->
                        scope.launch {
                            snackbar.showSnackbar(if (ok) "导入成功" else "导入失败：文件格式不对")
                        }
                    }
                }.onFailure { scope.launch { snackbar.showSnackbar("读取失败：${it.message}") } }
            }
        }
    }

    val p1 = all.filter { it.owner == "P1" }
    val p2 = all.filter { it.owner == "P2" }
    val shown = when (tab) {
        0 -> p1.filter { f1 -> p2.any { it.given == f1.given } }
        1 -> p1
        else -> p2
    }.distinctBy { it.given }

    voteTarget?.let { target ->
        VoteDialog(
            given = target.given,
            votes = votes.filter { it.given == target.given },
            onDismiss = { voteTarget = null },
            onSubmit = { voter, attitude, comment ->
                vm.addVote(target.given, voter, attitude, comment)
                voteTarget = null
            },
        )
    }

    androidx.compose.material3.Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Text(
                "收藏",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            TabRow(selectedTabIndex = tab) {
                listOf("共同喜欢", "家长一", "家长二").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            Text(
                "把导出文件发给对方导入，即可互相看到收藏；两人都收藏的名字出现在「共同喜欢」。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = {
                    pendingExport = vm.buildExport(profile?.toDomain(), all, votes)
                    exportLauncher.launch("jiaming-export.json")
                }, modifier = Modifier.weight(1f)) { Text("导出") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                    modifier = Modifier.weight(1f),
                ) { Text("导入") }
            }
            Spacer(Modifier.height(8.dp))
            if (shown.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (tab == 0) "共同喜欢还空着" else "这里还没有收藏",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "去候选列表点开名字，在证据面板收藏。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                    items(shown, key = { "${it.owner}-${it.given}" }) { f ->
                        FavoriteCard(
                            f = f,
                            votes = votes.filter { it.given == f.given },
                            onVote = { voteTarget = f },
                            onFinalize = { vm.toggleFinalize(f) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    f: FavoriteEntity,
    votes: List<VoteEntity>,
    onVote: () -> Unit,
    onFinalize: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = if (f.finalized) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.surname + f.given, fontSize = 22.sp, modifier = Modifier.weight(1f))
                val badge = when {
                    f.finalized -> "已定名"
                    f.failCount > 0 -> "${f.failCount} 硬伤"
                    f.warnCount > 0 -> "${f.warnCount} 注意"
                    else -> "全过"
                }
                Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            f.originText?.let {
                Text(
                    "「$it」——${f.originBook}·${f.originChapter}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val checks = runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString<List<CheckResult>>(f.checksJson)
            }.getOrDefault(emptyList())
            checks.filter { it.level != com.xiaojiaoyin.jiaming.domain.model.CheckLevel.PASS }
                .take(2)
                .forEach { c ->
                    Text("· ${c.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            if (votes.isNotEmpty()) {
                Text(
                    votes.joinToString("　") { "${it.voter}:${it.attitude}" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onVote) { Text("家人意见 (${votes.size})") }
                Spacer(Modifier.weight(1f))
                Text("定名", style = MaterialTheme.typography.labelMedium)
                Switch(checked = f.finalized, onCheckedChange = { onFinalize() })
            }
        }
    }
}

@Composable
private fun VoteDialog(
    given: String,
    votes: List<VoteEntity>,
    onDismiss: () -> Unit,
    onSubmit: (voter: String, attitude: String, comment: String) -> Unit,
) {
    var voter by remember { mutableStateOf("") }
    var attitude by remember { mutableStateOf("支持") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「$given」家人意见") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (votes.isNotEmpty()) {
                    votes.forEach {
                        Text("· ${it.voter} ${it.attitude}" + if (it.comment.isNotBlank()) "：${it.comment}" else "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(value = voter, onValueChange = { voter = it.take(8) }, label = { Text("称呼（奶奶/姑姑…）") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("支持", "反对", "待定").forEach { a ->
                        FilterChip(selected = attitude == a, onClick = { attitude = a }, label = { Text(a) })
                    }
                }
                OutlinedTextField(value = comment, onValueChange = { comment = it.take(60) }, label = { Text("留言（可选）") })
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(voter, attitude, comment) }) { Text("登记") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
