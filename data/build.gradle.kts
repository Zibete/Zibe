import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zibete.proyecto1.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.datastore.preferences)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.firebase.ui.auth)
    implementation(libs.facebook.login)
    implementation(libs.hilt.android)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)
    implementation(libs.javax.inject)
}
