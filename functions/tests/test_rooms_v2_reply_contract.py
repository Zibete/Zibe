from __future__ import annotations

import ast
import unittest
from pathlib import Path

FUNCTIONS_DIR = Path(__file__).resolve().parents[1]


class RoomsV2ReplyCallableContractTest(unittest.TestCase):
    def assert_reply_wiring(self, file_name: str, callable_name: str, state_helper: str) -> None:
        source = (FUNCTIONS_DIR / file_name).read_text(encoding="utf-8")
        tree = ast.parse(source, filename=file_name)
        callable_node = next(
            node
            for node in tree.body
            if isinstance(node, ast.FunctionDef) and node.name == callable_name
        )
        callable_source = ast.get_source_segment(source, callable_node) or ""

        self.assertIn('data.get("replyToMessageId")', callable_source)
        self.assertIn('field="replyToMessageId"', callable_source)
        self.assertIn(state_helper, callable_source)
        self.assertIn("reply_to_message_id=reply_to_message_id", callable_source)

    def test_public_callable_forwards_reply_target(self):
        self.assert_reply_wiring(
            "rooms_v2.py",
            "send_room_v2_text",
            "send_public_text_with_reply_state",
        )

    def test_private_callable_forwards_reply_target(self):
        self.assert_reply_wiring(
            "rooms_v2_private.py",
            "send_room_private_v2_text",
            "send_private_text_with_reply_state",
        )


if __name__ == "__main__":
    unittest.main()
