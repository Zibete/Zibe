"""Firebase Functions entrypoint.

Legacy functions stay byte-for-byte in ``legacy_main.py`` while RoomsV2 is developed
behind isolated paths and callable endpoints. The module object is aliased to the legacy
module so existing tests that patch ``functions.main`` continue patching the globals used
by the legacy trigger functions.
"""

from __future__ import annotations

import sys

try:
    from . import legacy_main as _legacy_main
except ImportError:
    import legacy_main as _legacy_main

# Older unit tests provide a deliberately tiny firebase_functions stub without https_fn.
# Skip V2 registration only in that synthetic environment; real Functions discovery and
# the emulator expose https_fn and therefore attach the new callables below.
try:
    from firebase_functions import https_fn as _https_fn  # noqa: F401
except ImportError:
    pass
else:
    try:
        from .rooms_v2 import create_room_v2, join_room_v2, leave_room_v2, send_room_v2_text
    except ImportError:
        from rooms_v2 import create_room_v2, join_room_v2, leave_room_v2, send_room_v2_text

    _legacy_main.create_room_v2 = create_room_v2
    _legacy_main.join_room_v2 = join_room_v2
    _legacy_main.leave_room_v2 = leave_room_v2
    _legacy_main.send_room_v2_text = send_room_v2_text

sys.modules[__name__] = _legacy_main
