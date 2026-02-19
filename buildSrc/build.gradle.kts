plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", "2.3.0"))
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.31.0")
}
