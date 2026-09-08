from __future__ import annotations

import json
from pathlib import Path
from typing import Any

REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
LEGACY_RULES = REPOSITORY_ROOT / "database.rules.json"
ROOMS_V2_RULES = REPOSITORY_ROOT / "database.roomsv2.rules.json"
OUTPUT = REPOSITORY_ROOT / "build/generated/firebase/database.rooms-combined.rules.json"


class RulesMergeError(RuntimeError):
    pass


def _load(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise RulesMergeError(f"Cannot read rules file {path}: {exc}") from exc
    if not isinstance(value, dict) or not isinstance(value.get("rules"), dict):
        raise RulesMergeError(f"Rules file must contain an object at 'rules': {path}")
    return value


def _merge(base: dict[str, Any], addition: dict[str, Any], *, path: str) -> dict[str, Any]:
    merged = dict(base)
    for key, incoming in addition.items():
        current_path = f"{path}/{key}" if path else key
        if key not in merged:
            merged[key] = incoming
            continue
        existing = merged[key]
        if isinstance(existing, dict) and isinstance(incoming, dict):
            merged[key] = _merge(existing, incoming, path=current_path)
            continue
        if existing == incoming:
            continue
        raise RulesMergeError(
            f"Conflicting rule at {current_path}: legacy={existing!r}, roomsV2={incoming!r}"
        )
    return merged


def build_combined_rules(
    legacy_path: Path = LEGACY_RULES,
    rooms_v2_path: Path = ROOMS_V2_RULES,
    output_path: Path = OUTPUT,
) -> Path:
    legacy = _load(legacy_path)
    rooms_v2 = _load(rooms_v2_path)
    combined = {
        **legacy,
        "rules": _merge(legacy["rules"], rooms_v2["rules"], path="rules"),
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(combined, ensure_ascii=False, indent=2, sort_keys=False) + "\n"
    output_path.write_text(serialized, encoding="utf-8")

    # Determinism guard: parsing and serializing the generated result must be stable.
    parsed = json.loads(output_path.read_text(encoding="utf-8"))
    if parsed != combined:
        raise RulesMergeError("Generated combined Rules did not round-trip deterministically")
    return output_path


def main() -> int:
    path = build_combined_rules()
    print(path.relative_to(REPOSITORY_ROOT).as_posix())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
