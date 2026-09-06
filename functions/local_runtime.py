"""Fail-closed emulator boundary; normal Firebase execution has no test exports."""

from __future__ import annotations

import hashlib
import json
import os
import time
import uuid
from collections.abc import Mapping
from urllib.parse import parse_qs, urlparse

PROJECT_ID = "demo-zibe-rooms"
DATABASE_NAMESPACE = f"{PROJECT_ID}-default-rtdb"
REQUIRED_ENDPOINTS = {
    "FIREBASE_AUTH_EMULATOR_HOST": "127.0.0.1:9099",
    "FIREBASE_DATABASE_EMULATOR_HOST": "127.0.0.1:9000",
    "FIREBASE_STORAGE_EMULATOR_HOST": "127.0.0.1:9199",
    "STORAGE_EMULATOR_HOST": "http://127.0.0.1:9199",
}


def is_local_runtime(environment: Mapping[str, str] | None = None) -> bool:
    """Validate every endpoint before any local Admin SDK operation is possible."""
    env = os.environ if environment is None else environment
    projects = [env.get(key, "") for key in ("GCLOUD_PROJECT", "GOOGLE_CLOUD_PROJECT")]
    config_text = env.get("FIREBASE_CONFIG", "")
    requested = (
        bool(env.get("FUNCTIONS_EMULATOR"))
        or bool(env.get("ZIBE_LOCAL_BACKEND"))
        or any(env.get(key) for key in REQUIRED_ENDPOINTS)
        or any(project.startswith("demo-") for project in projects)
        or '"demo-' in config_text
    )
    if not requested:
        return False
    if env.get("FUNCTIONS_EMULATOR") != "true":
        raise RuntimeError("Local backend requires the Functions emulator")
    if not any(projects) or any(project and project != PROJECT_ID for project in projects):
        raise RuntimeError("Local backend requires the dedicated demo project")
    for key, endpoint in REQUIRED_ENDPOINTS.items():
        if env.get(key) != endpoint:
            raise RuntimeError(f"Local backend requires the configured loopback endpoint: {key}")
    if config_text:
        try:
            config = json.loads(config_text)
        except (ValueError, TypeError) as error:
            raise RuntimeError("Local FIREBASE_CONFIG must be inline demo configuration") from error
        if not isinstance(config, dict) or config.get("projectId") != PROJECT_ID:
            raise RuntimeError("Local FIREBASE_CONFIG must use the dedicated demo project")
        if config.get("storageBucket") not in (None, f"{PROJECT_ID}.appspot.com"):
            raise RuntimeError("Local FIREBASE_CONFIG must use the dedicated demo bucket")
        database_url = config.get("databaseURL")
        if database_url:
            parsed = urlparse(database_url)
            expected_local = (
                parsed.scheme == "http"
                and parsed.netloc == REQUIRED_ENDPOINTS["FIREBASE_DATABASE_EMULATOR_HOST"]
                and parse_qs(parsed.query).get("ns") == [DATABASE_NAMESPACE]
            )
            if not expected_local and database_url != f"https://{DATABASE_NAMESPACE}.firebaseio.com":
                raise RuntimeError("Local FIREBASE_CONFIG must use the dedicated demo database")
    return True


def require_local_runtime() -> None:
    if not is_local_runtime():
        raise RuntimeError("This operation is available only in the dedicated local backend")


def initialize_local_admin(initialize_app):
    """Never resolve Application Default Credentials for fictitious local users."""
    require_local_runtime()
    from firebase_admin.credentials import Base
    from google.auth.credentials import AnonymousCredentials

    class EmulatorCredential(Base):
        def get_credential(self):
            return AnonymousCredentials()

    return initialize_app(
        EmulatorCredential(),
        {
            "projectId": PROJECT_ID,
            "databaseURL": f"https://{DATABASE_NAMESPACE}.firebaseio.com",
            "storageBucket": f"{PROJECT_ID}.appspot.com",
        },
    )


def record_local_push(database, *, token: str, data_payload: dict) -> str:
    """Record transport intent in the emulator; this is neither delivery nor receipt."""
    require_local_runtime()
    event_id = uuid.uuid4().hex
    database.reference(f"LocalTest/PushEvents/{event_id}").set(
        {
            "transport": "emulated",
            "tokenFingerprint": hashlib.sha256(token.encode("utf-8")).hexdigest(),
            "data": data_payload,
            "recordedAt": int(time.time() * 1000),
        }
    )
    return f"local:{event_id}"
