plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
        ndkVersion = "25.2.9519639"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
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
}

dependencies {

    // AndroidX base
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-rc01")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.navigation:navigation-fragment:2.9.6")
    implementation("androidx.navigation:navigation-ui:2.9.6")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")
    implementation("com.google.android.material:material:1.13.0")

    // Compose principal
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation:1.9.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime")

    // Activity con soporte Compose
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // BlurView
    implementation("com.github.Dimezis:BlurView:version-3.1.0")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-basement:18.9.0")

    // Firebase con BoM
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-messaging-directboot")

    implementation("com.firebaseui:firebase-ui-auth:9.1.1")

    // Glide + transforms
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.room:room-ktx:2.8.4")

    // GPUImage
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // FAB
    implementation("com.github.clans:fab:1.6.4")

    // Volley
    implementation("com.android.volley:volley:1.2.1")

    // Lottie
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.airbnb.android:lottie-compose:6.7.1")

    // UI/Imágenes
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.github.rahimlis:badgedtablayout:v1.2")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    implementation("com.github.CanHub:Android-Image-Cropper:4.3.3") {
        version { strictly("4.3.3") }
    }

    // Facebook Login
    implementation("com.facebook.android:facebook-login:18.1.3")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // libs locales
    implementation(fileTree("libs") { include("*.jar") })
}
