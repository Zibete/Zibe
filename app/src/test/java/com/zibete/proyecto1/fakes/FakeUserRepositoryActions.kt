package com.zibete.proyecto1.fakes

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserRepositoryActions

class FakeUserRepositoryActions : UserRepositoryActions {

    var createUserNodeCalled: Boolean = false
    var lastCreatedUser: FirebaseUser? = null
    var lastBirthDate: String? = null
    var lastDescription: String? = null

    var setLastSeenCalled: Boolean = false
    var lastActivityStatus: String? = null

    override suspend fun createUserNode(firebaseUser: FirebaseUser, birthDate: String, description: String) {
        createUserNodeCalled = true
        lastCreatedUser = firebaseUser
        lastBirthDate = birthDate
        lastDescription = description
    }

    override suspend fun setUserLastSeen() {
        setLastSeenCalled = true
    }

    override suspend fun setUserActivityStatus(status: String) {
        lastActivityStatus = status
    }
}
