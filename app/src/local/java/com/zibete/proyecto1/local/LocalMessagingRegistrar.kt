package com.zibete.proyecto1.local

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.components.Component
import com.google.firebase.components.ComponentRegistrar
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal

/** The pinned Functions SDK requires IID even when its optional FCM header is absent. */
class LocalMessagingRegistrar : ComponentRegistrar {
    override fun getComponents(): List<Component<*>> = listOf(
        Component.builder(FirebaseInstanceIdInternal::class.java)
            .factory { LocalMessagingIdentity() }
            .build()
    )
}

private class LocalMessagingIdentity : FirebaseInstanceIdInternal {
    override fun getToken(): String? = null

    override fun getTokenTask(): Task<String> = Tasks.forResult(null)

    override fun getId(): String =
        throw UnsupportedOperationException("FCM installation identity is disabled in the local variant")

    override fun deleteToken(authorizedEntity: String, scope: String): Unit =
        throw UnsupportedOperationException("The local variant has no remote FCM tokens")

    override fun addNewTokenListener(listener: FirebaseInstanceIdInternal.NewTokenListener): Unit =
        throw UnsupportedOperationException("FCM registration is disabled in the local variant")
}
