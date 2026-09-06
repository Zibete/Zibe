"""Authenticated callable exported only while the local emulator suite discovers main."""

from firebase_functions import https_fn

if __package__:
    from .local_runtime import PROJECT_ID, require_local_runtime
else:
    from local_runtime import PROJECT_ID, require_local_runtime


@https_fn.on_call(region="us-central1")
def local_backend_probe(request: https_fn.CallableRequest) -> dict:
    require_local_runtime()
    if request.auth is None or not request.auth.uid:
        raise https_fn.HttpsError(
            https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            "Authenticate a fictitious account with the local Auth emulator.",
        )
    if request.auth.token.get("aud") != PROJECT_ID:
        raise https_fn.HttpsError(
            https_fn.FunctionsErrorCode.PERMISSION_DENIED,
            "The local demo account is required.",
        )
    if request.data not in (None, {}):
        raise https_fn.HttpsError(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "The local probe accepts an empty object.",
        )
    return {
        "ok": True,
        "authenticated": True,
        "projectId": PROJECT_ID,
        "services": ["auth", "database", "storage", "functions"],
    }
