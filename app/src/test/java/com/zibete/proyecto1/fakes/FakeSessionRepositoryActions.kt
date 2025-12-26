package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.SessionRepositoryActions

class FakeSessionRepositoryActions : SessionRepositoryActions {

    data class ActiveSessionCall(
        val uid: String,
        val installId: String,
        val fcmToken: String?
    )

    var lastSetActiveSessionCall: ActiveSessionCall? = null
    var clearedUid: String? = null

    override suspend fun setActiveSession(uid: String, installId: String, fcmToken: String?) {
        lastSetActiveSessionCall = ActiveSessionCall(uid, installId, fcmToken)
    }

    override suspend fun clearSession(uid: String) {
        clearedUid = uid
    }
}
