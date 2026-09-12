#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
嘉名 JiaMing 数据校验脚本
用途：提交数据前校验 assets/data 下全部 JSON 的一致性与格式，防止脏数据入库。
依赖：仅 Python 3 标准库。
用法：
    python scripts/build_data.py
退出码：0 = 全部通过；1 = 存在错误（清单见输出 [ERR]）。
校验项：
  1. 全部 JSON 可解析，且 version/source 字段存在（数据溯源纪律）
  2. 字库：拼音格式（音节+1~5 声调数字）、笔画为正整数、结构枚举合法、性别枚举合法
  3. trait_lexicon 词表 ⊆ 字库
  4. 典籍 picks 覆盖报告（缺字列出，2 字以上；缺字不阻断但必须知晓）
  5. 负面词：y 非空、strong 字段存在、拼音格式合法
  6. 方言：risky 引用的字在字库中（提示级）
  7. 爆款：names/chars 条目含 year/src 字段
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "data")
ERRORS = []
NOTES = []


def load(name):
    path = os.path.join(ROOT, name)
    if not os.path.exists(path):
        ERRORS.append(f"[ERR] 缺少数据文件 {name}")
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def check_meta(d, name):
    if d is None:
        return False
    if not d.get("version"):
        ERRORS.append(f"[ERR] {name} 缺 version")
    if not d.get("source"):
        ERRORS.append(f"[ERR] {name} 缺 source（数据溯源纪律）")
    return True


def main():
    # 1 字库
    chars = load("chars.json")
    if check_meta(chars, "chars.json"):
        lib = {}
        for c in chars.get("chars", []):
            ch = c.get("c", "")
            if len(ch) != 1:
                ERRORS.append(f"[ERR] 字库字段 c 非单字: {c}")
                continue
            lib[ch] = c
            y = c.get("y", "")
            if not re.fullmatch(r"[a-z]+[1-5]", y):
                ERRORS.append(f"[ERR] 字库拼音格式非法: {ch} -> {y}")
            if not isinstance(c.get("st"), int) or c.get("st", 0) <= 0:
                ERRORS.append(f"[ERR] 字库笔画非法: {ch}")
            if c.get("gx") not in ("du", "lr", "ud", "wrap"):
                ERRORS.append(f"[ERR] 字库结构非法: {ch} -> {c.get('gx')}")
            if c.get("g") not in ("m", "f", "n"):
                ERRORS.append(f"[ERR] 字库性别非法: {ch} -> {c.get('g')}")
        NOTES.append(f"字库 {len(lib)} 字")

    # 2 词表 ⊆ 字库
    lex = load("trait_lexicon.json")
    if check_meta(lex, "trait_lexicon.json") and chars:
        for trait, words in lex.get("traits", {}).items():
            for w in words:
                if w not in lib:
                    ERRORS.append(f"[ERR] 词表「{trait}」含字库外字: {w}")
        NOTES.append(f"词表 {len(lex.get('traits', {}))} 个标签")

    # 3 典籍
    classics = load("classics.json")
    if check_meta(classics, "classics.json") and chars:
        for e in classics.get("entries", []):
            if not e.get("text") or not e.get("gloss"):
                ERRORS.append(f"[ERR] 典籍条目缺 text/gloss: {e.get('chapter')}")
            for w in e.get("picks", []):
                missing = [ch for ch in w if ch not in lib]
                if missing:
                    NOTES.append(f"[NOTE] 典籍 {e.get('chapter')} 挑词「{w}」缺字: {'、'.join(missing)}（不阻断，需知晓）")
        NOTES.append(f"典籍 {len(classics.get('entries', []))} 条")

    # 4 负面词
    neg = load("negative_words.json")
    if check_meta(neg, "negative_words.json"):
        for w in neg.get("words", []):
            if not w.get("y"):
                ERRORS.append(f"[ERR] 负面词缺读音: {w.get('w')}")
            if not isinstance(w.get("strong"), bool):
                ERRORS.append(f"[ERR] 负面词缺 strong 分级: {w.get('w')}")
            for y in w.get("y", []):
                if not re.fullmatch(r"[a-z]+[1-5]", y):
                    ERRORS.append(f"[ERR] 负面词拼音格式非法: {w.get('w')} -> {y}")
        NOTES.append(f"负面词 {len(neg.get('words', []))} 条")

    # 5 方言
    dia = load("dialects.json")
    if check_meta(dia, "dialects.json") and chars:
        for d in dia.get("dialects", []):
            if not d.get("confidence"):
                ERRORS.append(f"[ERR] 方言缺置信度: {d.get('name')}")
            for r in d.get("risky", []):
                if r.get("c") not in lib:
                    NOTES.append(f"[NOTE] 方言「{d.get('name')}」引用字库外字: {r.get('c')}")
        NOTES.append(f"方言 {len(dia.get('dialects', []))} 种")

    # 6 爆款
    trend = load("trend.json")
    if check_meta(trend, "trend.json"):
        for n in trend.get("names", []) + trend.get("chars", []):
            if not n.get("year") or not n.get("src"):
                ERRORS.append(f"[ERR] 爆款条目缺 year/src: {n}")
        NOTES.append(f"爆款名 {len(trend.get('names', []))} / 字 {len(trend.get('chars', []))}")

    for n in NOTES:
        print(n)
    for e in ERRORS:
        print(e)
    print(f"== 校验{'通过' if not ERRORS else '失败'} ==")
    sys.exit(1 if ERRORS else 0)


if __name__ == "__main__":
    main()
