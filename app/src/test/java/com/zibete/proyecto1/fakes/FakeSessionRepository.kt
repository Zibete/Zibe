package com.zibete.proyecto1.fakes

import com.google.firebase.database.DataSnapshot
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario

class FakeSessionRepositoryActions(
    private val scenarioProvider: () -> TestScenario
) : SessionRepositoryActions {

    var lastSetActiveSessionCall: ActiveSessionCall? = null
    var clearedUid: String? = null

    override suspend fun setActiveSession(uid: String, installId: String, fcmToken: String?) {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
        lastSetActiveSessionCall = ActiveSessionCall(uid, installId, fcmToken)
    }

    override suspend fun clearSession(uid: String) {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
        clearedUid = uid
    }
}

class FakeSessionRepositoryProvider(
    private val scenarioProvider: () -> TestScenario
) : SessionRepositoryProvider {

    override suspend fun getLocalInstallId(): String =
        TestData.INSTALL_ID

    override suspend fun getLocalFcmToken(): String =
        TestData.TOKEN

    override suspend fun getInstallId(uid: String): String? {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
        return null
    }

    override suspend fun getFcmToken(uid: String): String? {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
        return null
    }

    override suspend fun getSessionsByFcmToken(token: String): DataSnapshot =
        throw NotImplementedError("Not needed for SessionBootstrapper tests")
}
