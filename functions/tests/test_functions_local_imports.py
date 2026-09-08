from __future__ import annotations

import ast
import unittest
from pathlib import Path

FUNCTIONS_DIR = Path(__file__).resolve().parents[1]


class FunctionsLocalImportContractTest(unittest.TestCase):
    def test_relative_imports_reference_existing_local_modules(self):
        failures: list[str] = []
        for source_path in sorted(FUNCTIONS_DIR.glob("*.py")):
            tree = ast.parse(source_path.read_text(encoding="utf-8"), filename=str(source_path))
            for node in ast.walk(tree):
                if not isinstance(node, ast.ImportFrom) or node.level != 1 or not node.module:
                    continue
                module_path = FUNCTIONS_DIR / (node.module.replace(".", "/") + ".py")
                package_path = FUNCTIONS_DIR / node.module.replace(".", "/") / "__init__.py"
                if not module_path.exists() and not package_path.exists():
                    failures.append(
                        f"{source_path.name}: relative import .{node.module} has no local module"
                    )
        self.assertEqual([], failures, "\n".join(failures))


if __name__ == "__main__":
    unittest.main()
