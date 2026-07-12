#!/usr/bin/env python3
"""Fail when module or presentation boundaries regress."""

from __future__ import annotations

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
IMPORT = re.compile(r"^import\s+([^\s]+)", re.MULTILINE)

DOMAIN_FORBIDDEN = (
    "android.",
    "androidx.",
    "com.facebook.",
    "com.google.firebase.",
)

PRESENTATION_FORBIDDEN = (
    "com.google.firebase.",
    "com.zibete.proyecto1.di.firebase.",
    "com.zibete.proyecto1.data.ChatRefs",
    "com.zibete.proyecto1.data.ChatRepository",
    "com.zibete.proyecto1.data.GroupRepository",
    "com.zibete.proyecto1.data.LocationRepository",
    "com.zibete.proyecto1.data.PresenceRepository",
    "com.zibete.proyecto1.data.SessionRepository",
    "com.zibete.proyecto1.data.UserRepository",
)

VIEWMODEL_FORBIDDEN = (
    "android.",
)

def kotlin_files(path: Path):
    yield from path.rglob("*.kt")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def check_imports(path: Path, forbidden: tuple[str, ...], allow: set[str] | None = None):
    failures: list[str] = []
    allow = allow or set()
    for source in kotlin_files(path):
        source_path = relative(source)
        imports = IMPORT.findall(source.read_text(encoding="utf-8"))
        for imported in imports:
            is_forbidden = any(
                imported.startswith(rule) if rule.endswith(".") else imported == rule
                for rule in forbidden
            )
            if is_forbidden and source_path not in allow:
                failures.append(f"{source_path}: forbidden import {imported}")
    return failures


def check_viewmodels(path: Path):
    failures: list[str] = []
    for source in path.rglob("*ViewModel.kt"):
        for imported in IMPORT.findall(source.read_text(encoding="utf-8")):
            if any(imported.startswith(rule) for rule in VIEWMODEL_FORBIDDEN):
                failures.append(f"{relative(source)}: forbidden ViewModel import {imported}")
    return failures


def main() -> int:
    failures = check_imports(ROOT / "domain" / "src", DOMAIN_FORBIDDEN)
    failures += check_imports(
        ROOT / "app" / "src" / "main" / "java" / "com" / "zibete" / "proyecto1" / "ui",
        PRESENTATION_FORBIDDEN,
    )
    failures += check_viewmodels(
        ROOT / "app" / "src" / "main" / "java" / "com" / "zibete" / "proyecto1" / "ui"
    )
    if failures:
        print("Architecture boundary violations:")
        print("\n".join(f"- {failure}" for failure in failures))
        return 1
    print("Architecture boundaries OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
