import io.gierla.utils.Dependencies
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {

    compileSdkVersion(Dependencies.AndroidSdk.compileSdkVersion)

    defaultConfig {
        applicationId =  Dependencies.AndroidSdk.Sample.applicationId
        minSdkVersion(Dependencies.AndroidSdk.minSdkVersion)
        targetSdkVersion(Dependencies.AndroidSdk.targetSdkVersion)
        versionCode = Dependencies.AndroidSdk.Sample.versionCode
        versionName = Dependencies.AndroidSdk.Sample.versionName
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility =  JavaVersion.VERSION_1_8
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }
}

dependencies {
    // Local libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Dependencies.kotlinVersion}")

    // Compatibility
    implementation("androidx.appcompat:appcompat:${Dependencies.Libraries.AndroidX.appCompat}")

    // AndroidX
    implementation("androidx.core:core-ktx:${Dependencies.Libraries.AndroidX.coreKtx}")

    // Flat layout widget
    implementation("androidx.constraintlayout:constraintlayout:${Dependencies.Libraries.AndroidX.constraintLayout}")

    // Concurrent coding
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Dependencies.Libraries.coroutinesCore}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Dependencies.Libraries.coroutinesCore}")

    // Library
    implementation(project(":core"))
    //implementation("io.gierla.reactivecomponents:core:0.0.22")
    kapt(project(":annotationprocessor"))
    //kapt("io.gierla.reactivecomponents:annotations:0.0.22")
}
