plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.example.desktop.DesktopMainKt")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.desktop.DesktopMainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
