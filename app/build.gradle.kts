import java.io.FileInputStream
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val facebookAppId = localProperties.getProperty("FACEBOOK_APP_ID")?.trim().orEmpty()
val facebookClientToken = localProperties.getProperty("FACEBOOK_CLIENT_TOKEN")?.trim().orEmpty()
val fbLoginProtocolScheme = "fb${facebookAppId.lowercase()}"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.zibete.proyecto1"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zibete.proyecto1"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.10"

        multiDexEnabled = true
        testInstrumentationRunner = "com.zibete.proyecto1.HiltTestRunner"

        val webClientId = (localProperties.getProperty("WEB_CLIENT_ID")
            ?: System.getenv("WEB_CLIENT_ID")
            ?: "").trim()

        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        resValue("string", "facebook_app_id", facebookAppId)
        resValue("string", "facebook_client_token", facebookClientToken)
        resValue("string", "fb_login_protocol_scheme", fbLoginProtocolScheme)
    }

    testOptions {
        animationsDisabled = true
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        disable += listOf(
            "CoroutineCreationDuringComposition",
            "StateFlowValueCalledInComposition"
        )
    }
}

gradle.taskGraph.whenReady {
    val hasReleaseTask = allTasks.any { it.name.contains("release", ignoreCase = true) }
    if (!hasReleaseTask) return@whenReady

    if (facebookAppId.isBlank() || facebookAppId.equals("CHANGE_ME", ignoreCase = true)) {
        throw GradleException("FACEBOOK_APP_ID must be set for release builds.")
    }
    if (facebookClientToken.isBlank() || facebookClientToken.equals("CHANGE_ME", ignoreCase = true)) {
        throw GradleException("FACEBOOK_CLIENT_TOKEN must be set for release builds.")
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.android.material:material:1.13.0")
    }
}


dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // -------------------------------
    // ANDROIDX BASE
    // -------------------------------
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager:viewpager:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
//    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.annotation:annotation:1.9.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // -------------------------------
    // COMPOSE
    // -------------------------------
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.test.espresso:espresso-intents:3.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // -------------------------------
    // LIFECYCLE
    // -------------------------------
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")

    // -------------------------------
    // DATASTORE
    // -------------------------------
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // -------------------------------
    // HILT (KSP)
    // -------------------------------
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.58")

    androidTestImplementation("com.google.dagger:hilt-android-testing:2.59.2")
    androidTestImplementation("io.mockk:mockk-android:1.14.7")

    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.59.2")

    // -------------------------------
    // IMAGES / UI
    // -------------------------------
    implementation("com.github.Dimezis:BlurView:version-3.1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    // Glide
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    // Coil
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("id.zelory:compressor:3.0.1")
    implementation("com.github.rahimlis:badgedtablayout:v1.2")
    implementation("com.github.yalantis:ucrop:2.2.8")

    // -------------------------------
    // GOOGLE / FIREBASE
    // -------------------------------
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.facebook.android:facebook-login:17.0.0")

    // -------------------------------
    // OTHERS
    // -------------------------------

    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    implementation("com.github.clans:fab:1.6.4")

    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // -------------------------------
    // TESTING
    // -------------------------------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.14.7")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Local libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Unit tests (src/test)
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(kotlin("test"))

    // Android tests (src/androidTest)
    androidTestImplementation("org.mockito:mockito-android:5.12.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

}
