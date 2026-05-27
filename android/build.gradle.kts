import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion = "1.14.1"

// Separate configuration to fetch the per-ABI native .so libraries.
val natives: Configuration by configurations.creating

android {
    namespace = "com.aratetris.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aratetris"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Extract libGDX's prebuilt .so files from the gdx-platform jars into jniLibs/<abi>.
tasks.register("copyAndroidNatives") {
    val jniLibsDir = file("src/main/jniLibs")
    outputs.dir(jniLibsDir)
    doFirst {
        natives.files.forEach { jar ->
            // jar name e.g. gdx-platform-1.14.1-natives-arm64-v8a.jar  ->  abi = arm64-v8a
            val abi = jar.name.substringAfter("natives-").removeSuffix(".jar")
            val outDir = file("src/main/jniLibs/$abi")
            outDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outDir)
                include("*.so")
            }
        }
    }
}

tasks.configureEach {
    if (name.contains("merge") && name.contains("JniLibFolders")) {
        dependsOn("copyAndroidNatives")
    }
}
