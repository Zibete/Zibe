from __future__ import annotations

import json
import unittest
from unittest.mock import MagicMock, patch

from functions import local_runtime
from functions.tests.test_main import _load_main_module


def local_environment():
    return {
        "FUNCTIONS_EMULATOR": "true",
        "GCLOUD_PROJECT": local_runtime.PROJECT_ID,
        "GOOGLE_CLOUD_PROJECT": local_runtime.PROJECT_ID,
        **local_runtime.REQUIRED_ENDPOINTS,
    }


class LocalRuntimeIsolationTest(unittest.TestCase):
    def test_normal_production_environment_keeps_existing_runtime(self):
        self.assertFalse(local_runtime.is_local_runtime({"GCLOUD_PROJECT": "existing-project"}))

    def test_complete_demo_environment_is_accepted(self):
        self.assertTrue(local_runtime.is_local_runtime(local_environment()))

    def test_demo_environment_cannot_run_without_functions_emulator(self):
        env = local_environment()
        env.pop("FUNCTIONS_EMULATOR")
        with self.assertRaisesRegex(RuntimeError, "Functions emulator"):
            local_runtime.is_local_runtime(env)

    def test_non_demo_or_conflicting_project_is_rejected(self):
        for key in ("GCLOUD_PROJECT", "GOOGLE_CLOUD_PROJECT"):
            with self.subTest(key=key):
                env = local_environment()
                env[key] = "existing-project"
                with self.assertRaisesRegex(RuntimeError, "demo project"):
                    local_runtime.is_local_runtime(env)

    def test_missing_service_aborts_instead_of_falling_back_to_remote(self):
        for key in local_runtime.REQUIRED_ENDPOINTS:
            with self.subTest(key=key):
                env = local_environment()
                env.pop(key)
                with self.assertRaisesRegex(RuntimeError, key):
                    local_runtime.is_local_runtime(env)

    def test_remote_or_wrong_port_is_rejected_for_every_service(self):
        for key in local_runtime.REQUIRED_ENDPOINTS:
            for invalid in ("remote.example:9000", "127.0.0.1:9999"):
                with self.subTest(key=key, endpoint=invalid):
                    env = local_environment()
                    env[key] = invalid
                    with self.assertRaisesRegex(RuntimeError, key):
                        local_runtime.is_local_runtime(env)

    def test_missing_project_is_rejected(self):
        env = local_environment()
        env.pop("GCLOUD_PROJECT")
        env.pop("GOOGLE_CLOUD_PROJECT")
        with self.assertRaisesRegex(RuntimeError, "demo project"):
            local_runtime.is_local_runtime(env)

    def test_cli_demo_configuration_is_accepted(self):
        env = local_environment()
        env["FIREBASE_CONFIG"] = json.dumps({
            "projectId": local_runtime.PROJECT_ID,
            "storageBucket": "demo-zibe-rooms.appspot.com",
            "databaseURL": "http://127.0.0.1:9000/?ns=demo-zibe-rooms-default-rtdb",
        })
        self.assertTrue(local_runtime.is_local_runtime(env))

    def test_production_or_different_namespace_configuration_is_rejected(self):
        configurations = (
            {"projectId": "existing-project"},
            {"projectId": local_runtime.PROJECT_ID, "storageBucket": "existing.appspot.com"},
            {"projectId": local_runtime.PROJECT_ID, "databaseURL": "https://existing.firebaseio.com"},
            {"projectId": local_runtime.PROJECT_ID, "databaseURL": "http://127.0.0.1:9000/?ns=other"},
        )
        for config in configurations:
            with self.subTest(config=config):
                env = local_environment()
                env["FIREBASE_CONFIG"] = json.dumps(config)
                with self.assertRaises(RuntimeError):
                    local_runtime.is_local_runtime(env)

    def test_local_operation_is_denied_without_local_environment(self):
        with patch.dict("os.environ", {}, clear=True):
            with self.assertRaises(RuntimeError):
                local_runtime.require_local_runtime()

    def test_local_push_records_intent_without_storing_token(self):
        database = MagicMock()
        with patch.dict("os.environ", local_environment(), clear=True):
            result = local_runtime.record_local_push(
                database, token="fictitious-token", data_payload={"type": "room"}
            )
        self.assertTrue(result.startswith("local:"))
        self.assertTrue(database.reference.call_args.args[0].startswith("LocalTest/PushEvents/"))
        event = database.reference.return_value.set.call_args.args[0]
        self.assertEqual("emulated", event["transport"])
        self.assertEqual({"type": "room"}, event["data"])
        self.assertNotIn("fictitious-token", str(event))
        self.assertNotIn("received", event)

    def test_runtime_transport_never_calls_fcm_when_local(self):
        with patch.dict("os.environ", {}, clear=True):
            main = _load_main_module()
        with patch.dict("os.environ", local_environment(), clear=True):
            result = main._send_push(
                token="fictitious-token", data_payload={"type": "room"},
                include_notification=False,
            )
        self.assertTrue(result.startswith("local:"))
        main.messaging.Message.assert_not_called()
        main.messaging.send.assert_not_called()

    def test_runtime_transport_aborts_before_fcm_if_local_endpoint_is_missing(self):
        with patch.dict("os.environ", {}, clear=True):
            main = _load_main_module()
        env = local_environment()
        env.pop("FIREBASE_AUTH_EMULATOR_HOST")
        with patch.dict("os.environ", env, clear=True):
            with self.assertRaises(RuntimeError):
                main._send_push(
                    token="fictitious-token", data_payload={"type": "room"},
                    include_notification=False,
                )
        main.messaging.send.assert_not_called()

    def test_test_callable_is_not_exported_in_production_discovery(self):
        with patch.dict("os.environ", {}, clear=True):
            main = _load_main_module()
        self.assertFalse(hasattr(main, "local_backend_probe"))


if __name__ == "__main__":
    unittest.main()
