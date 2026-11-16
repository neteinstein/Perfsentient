#!/usr/bin/env python3
"""Turns a Kover/JaCoCo XML report into a Markdown table for the GitHub job summary."""
import sys
import xml.etree.ElementTree as ET


def line_counter(node):
    for counter in node.findall("counter"):
        if counter.get("type") == "LINE":
            missed, covered = int(counter.get("missed")), int(counter.get("covered"))
            return covered, missed + covered
    return 0, 0


def pct(covered, total):
    return 100.0 * covered / total if total else 100.0


def main(path):
    root = ET.parse(path).getroot()
    covered, total = line_counter(root)
    print("## 📊 Code coverage\n")
    print(f"**Total line coverage: {pct(covered, total):.1f}%** ({covered}/{total} lines)\n")
    print("| Package | Lines | Coverage |")
    print("|---|---:|---:|")
    rows = []
    for package in root.findall("package"):
        c, t = line_counter(package)
        if t:
            rows.append((package.get("name").replace("/", "."), c, t))
    for name, c, t in sorted(rows):
        print(f"| `{name}` | {c}/{t} | {pct(c, t):.1f}% |")


if __name__ == "__main__":
    main(sys.argv[1])
