import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.1.20"

    // Google services Gradle plugin
    id("com.google.gms.google-services")

    //id("dev.icerock.mobile.multiplatform-resources")

    //IOS Implementation
    //id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    /*
    EXCLUDING IOS TARGETS FOR NOW
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }*/
    /*
    cocoapods {
        version = "1.0.0"
        summary = "PlayScore Kotlin components"
        homepage = "https://github.com/pablisEsp/PlayScore"
        ios.deploymentTarget = "13.0" // Specify minimum iOS version
        framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        // Firebase dependencies
        pod("Firebase/Core") { version = "10.19.0" }
        pod("Firebase/Auth") { version = "10.19.0" }
        pod("Firebase/Database") { version = "10.19.0" }
    }*/

    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val commonMain by getting
        val androidMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // HTTP client for Android
            implementation(libs.ktor.client.okhttp)

            // Android lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Specific Android dependencies
            // Koin for Android
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // Firebase dependencies go here
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.12.0"))
            implementation("com.google.firebase:firebase-auth")
            implementation("com.google.firebase:firebase-database")

            // For Task.await() extension
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")

        }
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Ktor + JSON + coroutines
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Koin for KMP
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.navigation.compose)

            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")


        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)


            // HTTP client for desktop
            implementation(libs.ktor.client.cio)

            // Specific lifecycle dependencies for desktop for JVM
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)

            // Koin for desktop
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation("org.slf4j:slf4j-simple:1.7.36")
            implementation("io.ktor:ktor-client-logging:${libs.versions.ktor.get()}")

        }

    }
}

android {
    namespace = "com.playscore.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.playscore.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.material3.android)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.playscore.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.playscore.project"
            packageVersion = "1.0.0"
        }
    }
}
