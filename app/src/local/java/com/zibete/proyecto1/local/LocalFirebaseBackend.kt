package com.zibete.proyecto1.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

/** This entire configuration is excluded from debug/release source sets. */
object LocalFirebaseBackend {
    const val PROJECT_ID = "demo-zibe-rooms"
    const val DATABASE_NAMESPACE = "$PROJECT_ID-default-rtdb"
    const val HOST = "10.0.2.2"
    const val AUTH_PORT = 9099
    const val DATABASE_PORT = 9000
    const val STORAGE_PORT = 9199
    const val FUNCTIONS_PORT = 5001

    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        check(!initialized) { "Local Firebase must be initialized exactly once before first use" }
        check(context.packageName.endsWith(".local")) { "Local Firebase requires the isolated applicationId" }
        check(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            "Local Firebase cannot run in a non-debuggable application"
        }
        check(FirebaseApp.getApps(context).isEmpty()) {
            "Firebase was initialized before the local backend guard; refusing to continue"
        }
        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:1234567890:android:0000000000000000000000")
            .setApiKey("local-emulator-api-key")
            // Keep even repeated SDK getInstance() lookups on the emulator URL.
            .setDatabaseUrl("http://$HOST:$DATABASE_PORT?ns=$DATABASE_NAMESPACE")
            .setStorageBucket("$PROJECT_ID.appspot.com")
            .build()
        val app = FirebaseApp.initializeApp(context, options)
        app.setDataCollectionDefaultEnabled(false)

        FirebaseAuth.getInstance(app).useEmulator(HOST, AUTH_PORT)
        FirebaseDatabase.getInstance(app).apply {
            useEmulator(HOST, DATABASE_PORT)
            setPersistenceEnabled(false)
        }
        FirebaseStorage.getInstance(app).useEmulator(HOST, STORAGE_PORT)
        FirebaseFunctions.getInstance(app).useEmulator(HOST, FUNCTIONS_PORT)
        initialized = true
        requireInitialized()
    }

    fun requireInitialized() {
        check(initialized) { "The local Firebase initialization provider did not run" }
        val app = FirebaseApp.getInstance()
        check(app.options.projectId == PROJECT_ID) { "Remote Firebase project refused by local backend" }
        val databaseUrl = Uri.parse(FirebaseDatabase.getInstance(app).reference.toString())
        check(databaseUrl.scheme == "http" && databaseUrl.host == HOST && databaseUrl.port == DATABASE_PORT) {
            "Remote Realtime Database endpoint refused by local backend"
        }
    }
}
