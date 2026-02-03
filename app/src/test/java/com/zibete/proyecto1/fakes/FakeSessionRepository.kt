package com.zibete.proyecto1.fakes

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
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

    override fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = Unit
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) = Unit
        }
    }

    override fun removeSessionListener(uid: String, listener: ValueEventListener) = Unit
}
