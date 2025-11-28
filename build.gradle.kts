// Top-level build file
plugins {
    // 1. Android Gradle Plugin (La versión 8.13 NO existe, usamos la 8.3.1 estable)
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false

    // 2. Kotlin (Usamos 2.0.0 estable)
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false

    // 3. Google Services
    id("com.google.gms.google-services") version "4.4.1" apply false

    // 4. Compose Compiler (Debe coincidir con Kotlin)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // 5. HILT (Usamos 2.48 porque es compatible con tu Android Studio actual)
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}