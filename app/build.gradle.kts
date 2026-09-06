import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val facebookAppId = localProperties.getProperty("FACEBOOK_APP_ID")?.trim().orEmpty()
val facebookClientToken = localProperties.getProperty("FACEBOOK_CLIENT_TOKEN")?.trim().orEmpty()
val fbLoginProtocolScheme = "fb${facebookAppId.lowercase()}"
val localInstrumentation = providers.gradleProperty("roomsLocalTests")
    .orElse("false").get().toBooleanStrict()
val instrumentationBuildType = if (localInstrumentation) "local" else "debug"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.zibete.proyecto1"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zibete.proyecto1"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.10"

        multiDexEnabled = true
        testInstrumentationRunner = if (instrumentationBuildType == "local") {
            "androidx.test.runner.AndroidJUnitRunner"
        } else {
            "com.zibete.proyecto1.HiltTestRunner"
        }

        val webClientId = (localProperties.getProperty("WEB_CLIENT_ID")
            ?: System.getenv("WEB_CLIENT_ID")
            ?: "").trim()

        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        buildConfigField("boolean", "IS_LOCAL_BACKEND", "false")
        resValue("string", "facebook_app_id", facebookAppId)
        resValue("string", "facebook_client_token", facebookClientToken)
        resValue("string", "fb_login_protocol_scheme", fbLoginProtocolScheme)
    }

    testOptions {
        animationsDisabled = true
    }

    testBuildType = instrumentationBuildType
    if (localInstrumentation) sourceSets.getByName("androidTest").setRoot("src/androidTestSharedLocal")

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        create("local") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "IS_LOCAL_BACKEND", "true")
            buildConfigField("String", "WEB_CLIENT_ID", "\"\"")
            resValue("string", "facebook_app_id", "0")
            resValue("string", "facebook_client_token", "local-disabled")
            resValue("string", "fb_login_protocol_scheme", "zibe-local-disabled")
        }
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

// The local app creates FirebaseOptions itself before Application/Hilt starts.
// No production google-services.json is an input to this variant.
tasks.matching { it.name == "processLocalGoogleServices" }.configureEach {
    enabled = false
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // -------------------------------
    // ANDROIDX BASE
    // -------------------------------
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager)
    implementation(libs.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.concurrent.futures)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.navigation.compose)

    // -------------------------------
    // COMPOSE
    // -------------------------------
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.activity.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    "localImplementation"(libs.compose.ui.tooling)
    "localImplementation"(libs.compose.ui.test.manifest)

    // -------------------------------
    // LIFECYCLE
    // -------------------------------
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // -------------------------------
    // DATASTORE
    // -------------------------------
    implementation(libs.datastore.preferences)

    // -------------------------------
    // HILT (KSP)
    // -------------------------------
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // -------------------------------
    // IMAGES / UI
    // -------------------------------
    implementation(libs.blur.view)
    implementation(libs.circle.image.view)
    implementation(libs.photo.view)

    // Glide
    implementation(libs.glide)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)

    implementation(libs.compressor)
    implementation(libs.badge.tab.layout)
    implementation(libs.ucrop)

    // -------------------------------
    // GOOGLE / FIREBASE
    // -------------------------------
    implementation(libs.play.services.location)

    implementation(platform(libs.firebase.bom))
    implementation(libs.compose.material.icons)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck)
    releaseImplementation(libs.firebase.appcheck.play.integrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.firebase.ui.auth)
    implementation(libs.facebook.login)

    // -------------------------------
    // OTHERS
    // -------------------------------

    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)

    implementation(libs.graphics.path)
    implementation(libs.floating.action.button)

    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    implementation(libs.coroutines.play.services)

    // -------------------------------
    // TESTING
    // -------------------------------
    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(kotlin("test"))

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)

    // Local libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

}
