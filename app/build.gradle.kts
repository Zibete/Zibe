plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

// Configuración de Kapt fuera de Android block
kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.zibete.proyecto1"
    compileSdk = 36 // Usamos 34 estable. 35 puede dar problemas si no tienes el SDK bajado.

    defaultConfig {
        applicationId = "com.zibete.proyecto1"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.10"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        renderscriptSupportModeEnabled = true
        renderscriptTargetApi = 26 // 26 o superior
    }

    buildFeatures {
        viewBinding = true
        compose = true
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

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // -------------------------------
    // ANDROIDX BASE
    // -------------------------------
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager:viewpager:1.0.0")
    // Navigation (Versiones estables, 2.9 no existe)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.11.0")


    // -------------------------------
    // COMPOSE (BOM 2024.06.00 es la actual estable)
    // -------------------------------
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")

    // -------------------------------
    // LIFECYCLE
    // -------------------------------
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")


    // -------------------------------
    // HILT (Versión 2.48 obligatoria para compatibilidad)
    // -------------------------------
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.48")
    add("kapt", "com.google.dagger:hilt-compiler:2.48")

    // -------------------------------
    // UI LIBRARIES / IMAGES
    // -------------------------------
    implementation("com.github.Dimezis:BlurView:version-3.1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    // Glide (Versión 4.16.0 estable, la 5 es Alpha/Beta)
    implementation("com.github.bumptech.glide:glide:5.0.5")
    add("kapt", "com.github.bumptech.glide:compiler:4.16.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation("id.zelory:compressor:3.0.1")
    implementation("com.github.rahimlis:badgedtablayout:v1.2")

    // Coil
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    // ✅ REEMPLAZO MODERNO Y ESTABLE (UCrop)
    implementation("com.github.yalantis:ucrop:2.2.8")

    // -------------------------------
    // GOOGLE PLAY SERVICES & FIREBASE
    // -------------------------------
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // -------------------------------
    // OTHERS
    // -------------------------------
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    implementation("com.github.clans:fab:1.6.4")

    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    implementation("com.facebook.android:facebook-login:17.0.0")

    // -------------------------------
    // TESTING
    // -------------------------------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.14.7")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // Local Libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}