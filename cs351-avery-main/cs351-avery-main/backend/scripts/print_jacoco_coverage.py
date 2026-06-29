#!/usr/bin/env python3

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 2:
        print("[ERROR] Expected path to jacoco.xml", file=sys.stderr)
        return 1

    report_path = Path(sys.argv[1])
    if not report_path.exists():
        print(f"[ERROR] Coverage report not found: {report_path}", file=sys.stderr)
        return 1

    root = ET.parse(report_path).getroot()
    counter = next((node for node in root.findall("counter") if node.attrib.get("type") == "INSTRUCTION"), None)
    if counter is None:
        print("[ERROR] INSTRUCTION coverage counter missing from jacoco.xml", file=sys.stderr)
        return 1

    missed = float(counter.attrib["missed"])
    covered = float(counter.attrib["covered"])
    total = missed + covered
    ratio = (covered / total * 100.0) if total else 0.0
    print(f"[INFO] Instruction coverage: {ratio:.2f}% ({int(covered)}/{int(total)})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
