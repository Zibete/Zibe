"""Firebase Functions entrypoint.

The previous implementation lives unchanged in ``legacy_main.py``. Its functions are
rebound to this module's globals so Firebase discovery still sees them in ``main.py`` and
existing tests can keep patching helpers on ``functions.main``. RoomsV2 exports are then
added without changing any legacy trigger path.
"""

from __future__ import annotations

import inspect
import types

try:
    from . import legacy_main as _legacy_main
except ImportError:
    import legacy_main as _legacy_main

for _name, _value in vars(_legacy_main).items():
    if _name.startswith("__"):
        continue
    if inspect.isfunction(_value):
        _clone = types.FunctionType(
            _value.__code__,
            globals(),
            name=_value.__name__,
            argdefs=_value.__defaults__,
            closure=_value.__closure__,
        )
        _clone.__kwdefaults__ = _value.__kwdefaults__
        _clone.__annotations__ = dict(getattr(_value, "__annotations__", {}))
        _clone.__dict__.update(getattr(_value, "__dict__", {}))
        _clone.__doc__ = _value.__doc__
        _clone.__module__ = __name__
        _clone.__qualname__ = _value.__qualname__
        globals()[_name] = _clone
    else:
        globals()[_name] = _value

# Existing unit tests intentionally stub only db_fn. In that synthetic environment
# https_fn is absent; skip only the new callable registration there.
try:
    from firebase_functions import https_fn as _https_fn  # noqa: F401
except ImportError:
    pass
else:
    try:
        from .rooms_v2 import (
            create_room_v2,
            join_room_v2,
            leave_room_v2,
            mark_room_thread_read_v2,
            send_room_v2_text,
            set_room_notifications_v2,
            set_room_visible_thread_v2,
        )
        from .rooms_v2_moderation import (
            ban_room_member_v2,
            close_room_v2,
            edit_room_v2,
            kick_room_member_v2,
            remove_room_message_v2,
            report_room_message_v2,
            resolve_room_report_v2,
            set_room_moderator_v2,
            transfer_room_owner_v2,
        )
        from .rooms_v2_private import (
            block_room_private_v2,
            open_room_private_v2,
            send_room_private_v2_text,
        )
    except ImportError:
        from rooms_v2 import (
            create_room_v2,
            join_room_v2,
            leave_room_v2,
            mark_room_thread_read_v2,
            send_room_v2_text,
            set_room_notifications_v2,
            set_room_visible_thread_v2,
        )
        from rooms_v2_moderation import (
            ban_room_member_v2,
            close_room_v2,
            edit_room_v2,
            kick_room_member_v2,
            remove_room_message_v2,
            report_room_message_v2,
            resolve_room_report_v2,
            set_room_moderator_v2,
            transfer_room_owner_v2,
        )
        from rooms_v2_private import (
            block_room_private_v2,
            open_room_private_v2,
            send_room_private_v2_text,
        )
