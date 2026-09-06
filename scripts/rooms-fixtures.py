"""Reset only the dedicated emulator namespace and seed fictional development accounts."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.request
from pathlib import Path

PROJECT = "demo-zibe-rooms"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, choices=[PROJECT])
    parser.parse_args()
    with urllib.request.urlopen("http://127.0.0.1:4400/emulators", timeout=5) as response:
        hub = json.load(response)
    expected_ports = {"auth": 9099, "database": 9000, "storage": 9199, "functions": 5001}
    if any(hub.get(service, {}).get("port") != port for service, port in expected_ports.items()):
        raise RuntimeError("The complete dedicated backend must be running before reset")

    os.environ.update({
        "FUNCTIONS_EMULATOR": "true",
        "GCLOUD_PROJECT": PROJECT,
        "GOOGLE_CLOUD_PROJECT": PROJECT,
        "FIREBASE_AUTH_EMULATOR_HOST": "127.0.0.1:9099",
        "FIREBASE_DATABASE_EMULATOR_HOST": "127.0.0.1:9000",
        "FIREBASE_STORAGE_EMULATOR_HOST": "127.0.0.1:9199",
        "STORAGE_EMULATOR_HOST": "http://127.0.0.1:9199",
    })
    os.environ.pop("GOOGLE_APPLICATION_CREDENTIALS", None)
    os.environ.pop("FIREBASE_CONFIG", None)
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "functions"))
    from firebase_admin import auth, db, initialize_app, storage
    from local_runtime import initialize_local_admin

    initialize_local_admin(initialize_app)
    request = urllib.request.Request(
        f"http://127.0.0.1:9099/emulator/v1/projects/{PROJECT}/accounts", method="DELETE"
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        if response.status != 200:
            raise RuntimeError("Local Auth reset failed")
    db.reference("/").delete()
    bucket = storage.bucket()
    for blob in bucket.list_blobs():
        blob.delete()
    accounts = {}
    for short_name, name in (("alice", "Alicia Local"), ("bob", "Bruno Local"), ("carol", "Carla Local")):
        uid = f"fixture-{short_name}"
        email = f"{short_name}@rooms.example.test"
        auth.create_user(uid=uid, email=email, password="Rooms-local-123!", display_name=name, email_verified=True)
        accounts[uid] = {
            "id": uid, "name": name, "email": email, "birthDate": "1995-01-01", "age": 31,
            "createdAt": int(time.time() * 1000), "photoUrl": "", "isOnline": False,
            "description": "Cuenta ficticia del entorno local", "latitude": -34.6037, "longitude": -58.3816,
        }
    db.reference("Users/Accounts").set(accounts)
    print(f"Reset local completo: {PROJECT}; 3 cuentas ficticias, sin salas ni mensajes.")
    print("Usuarios: alice, bob, carol @rooms.example.test; contraseña ficticia: Rooms-local-123!")


if __name__ == "__main__":
    main()
