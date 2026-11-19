#!/usr/bin/env python3
"""Regenerate the Mermaid module graph in README.md and docs/architecture.md from the build files.

Usage: python3 scripts/module-graph.py          # rewrite the graphs between the marker comments
       python3 scripts/module-graph.py --check  # exit 1 if either file is out of date

Edges are the main (non-test) project dependencies declared in each module's build.gradle.kts, plus the
ones every feature module gets from the `anchor.android.feature` convention plugin.
"""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
START, END = "<!-- module-graph:start -->", "<!-- module-graph:end -->"
FEATURE_CONVENTION_DEPS = [":core:designsystem", ":core:ui"]


def module_path(build_file):
    return ":" + os.path.dirname(os.path.relpath(build_file, ROOT)).replace(os.sep, ":")


def accessor_to_path(accessor):
    return ":" + re.sub(r"([a-z])([A-Z])", lambda m: m.group(1) + "-" + m.group(2).lower(), accessor).replace(".", ":")


def edges():
    result = set()
    for build_file in glob.glob(os.path.join(ROOT, "**", "build.gradle.kts"), recursive=True):
        rel = os.path.relpath(build_file, ROOT)
        if rel.startswith("build-logic") or rel == "build.gradle.kts" or "/build/" in rel:
            continue
        module = module_path(build_file)
        text = open(build_file).read()
        for match in re.finditer(r"^\s*(?:api|implementation)\(projects\.([\w.]+)\)", text, re.M):
            result.add((module, accessor_to_path(match.group(1))))
        if "anchor.android.feature" in text:
            result.update((module, dep) for dep in FEATURE_CONVENTION_DEPS)
    return sorted(result)


def node_id(path):
    return path.strip(":").replace(":", "_").replace("-", "_")


def mermaid():
    graph = edges()
    settings = open(os.path.join(ROOT, "settings.gradle.kts")).read()
    modules = sorted(set(re.findall(r'^include\("([^"]+)"\)', settings, re.M)) | {m for edge in graph for m in edge})
    groups = {}
    for m in modules:
        parts = m.strip(":").split(":")
        groups.setdefault(parts[0] if len(parts) > 1 else "", []).append(m)
    lines = ["```mermaid", "graph TD"]
    for group, members in sorted(groups.items()):
        if group:
            lines.append(f"    subgraph {group}")
        for m in members:
            label = m.strip(":").split(":")[-1]
            lines.append(f"    {'    ' if group else ''}{node_id(m)}[{label}]")
        if group:
            lines.append("    end")
    lines += [f"    {node_id(a)} --> {node_id(b)}" for a, b in graph]
    lines.append("```")
    return "\n".join(lines)


def main():
    check = "--check" in sys.argv
    block = f"{START}\n{mermaid()}\n{END}"
    stale = []
    for rel in ["README.md", "docs/architecture.md"]:
        path = os.path.join(ROOT, rel)
        text = open(path).read()
        if START not in text or END not in text:
            sys.exit(f"{rel}: missing {START} / {END} markers")
        updated = re.sub(re.escape(START) + r".*?" + re.escape(END), lambda _: block, text, flags=re.S)
        if updated != text:
            stale.append(rel)
            if not check:
                open(path, "w").write(updated)
    if check and stale:
        sys.exit("Module graph out of date in: " + ", ".join(stale) + ". Run: python3 scripts/module-graph.py")
    print("up to date" if not stale else "updated: " + ", ".join(stale))


if __name__ == "__main__":
    main()
