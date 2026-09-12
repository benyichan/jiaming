# 贡献指南

## 铁律：数据贡献必须附来源

本项目对「数据可信」的要求高于一切。修改 `app/src/main/assets/data/` 或 `data-src/` 下任何数据文件时：

1. PR 描述必须包含 `来源` 字段：URL / 书名 / 官方报告名
2. 新增典籍条目：附权威版本链接（如 [ctext.org](https://ctext.org)），有出入的原文会被要求核对
3. 新增方言雷区：标注你的母语区与置信度；低置信度条目欢迎但必须标 `低`
4. 新增负面词：附真实案例（哪个姓+名组合踩了它）；同音字过多的词不会被接受

## 数据校验

提交前跑一遍：

```bash
python scripts/build_data.py
```

它会校验：词表 ⊆ 字库、典籍挑词覆盖并报告缺字、拼音格式（声调数字）、负面词 strong/weak 分级完整、方言条目引用的字在字库中。

## 代码

- Kotlin + Jetpack Compose，沿用 `data / domain / ui / navigation` 分层 + AppGraph 手工注入
- 规则改动必须同步更新 `GoldenCasesTest` 的正反例
- 不引入网络栈、不引入 Hilt、不加统计/广告 SDK——这是产品原则（见 README 设计原则）

## 提交信息

`feat:` 新功能 / `fix:` 修复 / `data:` 数据变更 / `docs:` 文档
