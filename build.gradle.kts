// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register<Zip>("packageSoleLuxeDistribution") {
    archiveFileName.set("soleluxe_full_distribution.zip")
    destinationDirectory.set(file("$rootDir"))

    // 1. Package final premium Android APK
    from(file("$rootDir/app/build/outputs/apk/debug/app-debug.apk")) {
        rename("app-debug.apk", "soleluxe-android-production.apk")
        into("android")
    }

    // 2. Package finalized iOS & Web PWA Source (Landing, Manifest, Service Worker)
    from(file("$rootDir/public")) {
        into("pwa_source")
    }

    // 3. Package premium Desktop Local Node Gateway executable runnable jar
    from(file("$rootDir/desktop/build/libs/desktop.jar")) {
        rename("desktop.jar", "soleluxe-desktop-gateway.jar")
        into("desktop")
    }
}

