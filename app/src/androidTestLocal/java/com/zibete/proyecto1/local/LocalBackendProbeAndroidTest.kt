package com.zibete.proyecto1.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.zibete.proyecto1.core.utils.ZibeApp
import dagger.hilt.android.EntryPointAccessors
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Real Application, Hilt graph, authenticated Android SDKs and deployed emulator Rules. */
@RunWith(AndroidJUnit4::class)
class LocalBackendProbeAndroidTest {
    @Test(timeout = 180_000)
    fun authenticatedRepositoryAndAllEmulatedServicesAreReachable() = runBlocking {
        withTimeout(150_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.zibete.proyecto1.local", context.packageName)
            assertTrue(context.applicationContext is ZibeApp)
            LocalFirebaseBackend.requireInitialized()
            listOf(
                LocalFirebaseBackend.AUTH_PORT,
                LocalFirebaseBackend.DATABASE_PORT,
                LocalFirebaseBackend.STORAGE_PORT,
                LocalFirebaseBackend.FUNCTIONS_PORT
            ).forEach { port ->
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(LocalFirebaseBackend.HOST, port), 5_000)
                }
            }

            val auth = FirebaseAuth.getInstance()
            auth.signOut()
            val email = "probe-${UUID.randomUUID()}@example.invalid"
            val user = checkNotNull(auth.createUserWithEmailAndPassword(email, "Local-probe-123!").await().user)
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                LocalBackendProbeEntryPoint::class.java
            )
            val provider = entryPoint.sessionProvider()
            val installationId = provider.getLocalInstallId()
            assertEquals(installationId, provider.getLocalInstallId())
            assertNull(provider.getLocalFcmToken())
            entryPoint.sessionActions().setActiveSession(user.uid, installationId, null)
            assertEquals(installationId, provider.getInstallId(user.uid))

            val forbidden = FirebaseDatabase.getInstance()
                .getReference("Sessions/another-fictional-account/activeInstallId")
            try {
                forbidden.setValue("forbidden-client-write").await()
                fail("Rules allowed writing another account's installation")
            } catch (expected: DatabaseException) {
                assertTrue(expected.message.orEmpty().contains("Permission denied", ignoreCase = true))
            }

            val attachment = FirebaseStorage.getInstance().reference
                .child("LocalTest/${user.uid}/probe.txt")
            val bytes = "Local emulator fixture".toByteArray(Charsets.UTF_8)
            val metadata = StorageMetadata.Builder().setContentType("text/plain").build()
            attachment.putBytes(bytes, metadata).await()
            assertArrayEquals(bytes, attachment.getBytes(1_024).await())

            val response = FirebaseFunctions.getInstance()
                .getHttpsCallable("local_backend_probe").call(emptyMap<String, Any>()).await()
            val result = response.data as Map<*, *>
            assertEquals(true, result["ok"])
            assertEquals(true, result["authenticated"])
            assertEquals(LocalFirebaseBackend.PROJECT_ID, result["projectId"])
            assertEquals(setOf("auth", "database", "storage", "functions"), (result["services"] as List<*>).toSet())

            attachment.delete().await()
            FirebaseDatabase.getInstance().getReference("Sessions/${user.uid}/activeInstallId")
                .removeValue().await()
            user.delete().await()
            auth.signOut()
        }
    }
}
