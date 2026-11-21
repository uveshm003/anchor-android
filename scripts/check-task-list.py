#!/usr/bin/env python3
"""Validate docs/task-list.md: every task id `T-<n>` appears exactly once as a task heading.

Usage: python3 scripts/check-task-list.py   (run by `./gradlew ciCheck` via checkTaskList)
"""
import os
import re
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TASK = re.compile(r"^- \[[ x]\] \*\*T-(\d+)\*\*", re.M)


def main():
    with open(os.path.join(ROOT, "docs", "task-list.md"), encoding="utf-8") as handle:
        text = handle.read()
    ids = [int(n) for n in TASK.findall(text)]
    duplicates = sorted(n for n, count in Counter(ids).items() if count > 1)
    if duplicates:
        sys.exit("docs/task-list.md: duplicate task ids " + ", ".join(f"T-{n}" for n in duplicates))
    print(f"task list ok: {len(ids)} tasks")


if __name__ == "__main__":
    main()
