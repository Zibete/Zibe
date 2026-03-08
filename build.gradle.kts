// Top-level build file
plugins {
    // 1. Android Gradle Plugin
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false

    // 2. Kotlin
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false

    // 3. Google Services
    id("com.google.gms.google-services") version "4.4.1" apply false

    // 4. Compose Compiler
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false

    // 5. HILT
    id("com.google.dagger.hilt.android") version "2.59.2" apply false

    // 6. KSP
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
