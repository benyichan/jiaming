# 数据来源与 License 审计

> 本项目的可信度建立在「每条数据可溯源」上。本文档列出全部内置数据的来源、更新方式与 license。
> `app/src/main/assets/data/` 下的每个 JSON 的 `source` 字段与本文一一对应。

## 一、字库 chars.json

- **内容**：约 350 个取名高频字，含拼音（带调）、笔画、结构、性别倾向、期望标签、多音字与规范字表标记
- **来源**：项目精校。拼音与笔画按《通用规范汉字字典》人工核对；「是否在通用规范汉字表内」依据国务院 2013 年发布的《通用规范汉字表》（政府公开文件）
- **诚实声明**：v0.1 覆盖高频字，非全量字库；`std=true` 的人工核对在覆盖范围内可信，扩库时应接入结构化上游数据并做脚本比对
- **扩库候选上游**（接入前需核对 license 与字段口径）：
  - [theajack/cnchar](https://github.com/theajack/cnchar)（MIT）：拼音/笔画/偏旁
  - [babyname/fate](https://github.com/babyname/fate) 的 hanzidata（核对协议）
  - [Unihan 数据库](https://www.unicode.org/charts/unihan.html)（Unicode 官方，kTotalStrokes 笔画等字段，免收学费使用）

## 二、期望标签词表 trait_lexicon.json

- **来源**：项目自建（MIT）。词表中的字必须是字库子集（`scripts/build_data.py` 校验）
- **初始种子**：部分意象字参考 `chinese-baby-naming` skill（MIT）

## 三、负面词库 negative_words.json

- **来源**：项目自建（MIT）。收录口径：真实取名场景的高频谐音雷点（经典案例级），**宁缺勿滥**
- **强/弱分级**：强禁忌（脏话/性/疾病/严重不雅）→ 淘汰；一般负面 → 提示
- **贡献规则**：增补必须附真实案例；同音字过多的词不收（防止误杀，详见 GLOSSARY「谐音判定口径」）

## 四、多音字白名单 poly_whitelist.json / 姓氏读音 surnames.json

- **来源**：项目自建（MIT）。姓氏读音以姓氏读法为准（任=rén、单=shàn）

## 五、典籍库 classics.json

- **原文**：《诗经》《楚辞》《论语》《周易》均为公版古籍，以通行本为准
- **白话释义（gloss）**：项目自写（MIT），非任何译本摘录
- **挑词（picks）**：人工精选可组名词组
- **欢迎校勘 PR**：原文有出入的，请附权威版本（如 ctext.org 中国哲学书电子化计划）链接

## 六、方言雷区 dialects.json

- **来源**：移植并精简自 [chinese-baby-naming skill](https://github.com/Seiya89757/naming-skill) v1.4.0（MIT）的 `references/dialect-pitfalls.md`
- **性质标注**：CONTESTED。这是「风险提示表」，非精确方言拟音；每个方言条目带置信度（高/中/低），App 内提示「请当地长辈念一遍」
- **已知边界**：湘语/赣语/晋语/客家话覆盖度低，欢迎母语者贡献（附来源）

## 七、爆款字库 trend.json

- **来源**：全部为官方公开发布，仅引用已公开数字：
  - 公安部户政管理研究中心《二〇二〇年全国姓名报告》《二〇二一年全国姓名报告》（mps.gov.cn）
  - 重庆市公安局《2024 年新生儿入户用名排行》（gaj.cq.gov.cn，2025-01）
  - 人民网四川频道《2025 年四川省新生儿"爆款"名字出炉》（sc.people.com.cn，2026-01）
  - 津云《多地公布新生儿爆款名字》（tjyun.com）、福州新闻网与连云港 2025 年度报道
  - 苏州新闻网《"子涵""梓萱"不流行了？这届新生名字藏着"中国式浪漫"》（2025-09）
- **再分发口径**：仅引用榜单条目并逐条注明出处，不整表复制官方数据
- **事实核查**：公安部全国版姓名报告仅公开至 2021 年度；2022 年起以各地公开数据为准

## 八、上游依赖

| 依赖 | 用途 | License |
|---|---|---|
| androidx.compose / room / datastore / navigation | UI 与存储 | Apache-2.0 |
| cn.6tail:lunar | 农历/干支/八字排盘 | MIT |
| kotlinx.serialization | 数据序列化 | Apache-2.0 |

## 九、明确不做

- 五格剖象法打分（原因见 [GLOSSARY](GLOSSARY.md)）
- 自建「全国重名数据库」（只做官方渠道跳转）
- 整表二次分发官方统计报告
