plugins {
    alias(libs.plugins.kotlinJvm)
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("dev.livin.instaloader.cli.MainKt")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.coroutines.core)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("kmpinstaloader.jar")
    mergeServiceFiles()
}
