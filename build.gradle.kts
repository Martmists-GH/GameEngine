plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.atomicfu")
}

group = "com.martmists"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/service/local/repositories/snapshots/content/")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.joml:joml:${Versions.joml}") {
        // Needed to grab the snapshot with a bunch of fixes
        isChanging = true
    }
    implementation(platform("org.lwjgl:lwjgl-bom:${Versions.lwjgl}"))
    for (module in arrayOf("", "-assimp", "-glfw", "-openal", "-opengl", "-stb", "-jemalloc")) {
        implementation("org.lwjgl", "lwjgl$module")
        runtimeOnly("org.lwjgl", "lwjgl$module", classifier = "natives-${Versions.lwjglNatives}")
    }

    implementation("io.github.spair:imgui-java-lwjgl3:${Versions.imgui}")
    implementation("io.github.spair:imgui-java-binding:${Versions.imgui}")
    implementation("io.github.spair:imgui-java-natives-linux:${Versions.imgui}")
}

kotlin {
    jvmToolchain(23)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}
