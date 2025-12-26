package com.zibete.proyecto1.fakes

import com.google.firebase.database.DataSnapshot
import com.zibete.proyecto1.data.SessionRepositoryProvider

class FakeSessionRepositoryProvider(
    var localInstallId: String = "install-1",
    var localFcmToken: String? = "fcm-1",
) : SessionRepositoryProvider {

    override suspend fun getLocalInstallId(): String = localInstallId
    override suspend fun getLocalFcmToken(): String? = localFcmToken

    override suspend fun getInstallId(uid: String): String? = null
    override suspend fun getFcmToken(uid: String): String? = null
    override suspend fun getSessionsByFcmToken(token: String): DataSnapshot =
        throw NotImplementedError("Not needed for SessionBootstrapper tests")
}
