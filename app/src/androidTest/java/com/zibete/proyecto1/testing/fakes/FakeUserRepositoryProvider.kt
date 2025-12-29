package com.zibete.proyecto1.testing.fakes

import com.zibete.proyecto1.data.UserRepositoryProvider

class FakeUserRepositoryProvider(
    var accountExists: Boolean = true,
    var hasBirthDate: Boolean = false
) : UserRepositoryProvider {

    override suspend fun accountExists(uid: String): Boolean = accountExists
    override suspend fun hasBirthDate(uid: String): Boolean = hasBirthDate
}
