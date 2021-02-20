import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {

    compileSdkVersion(AndroidSdk.compileSdkVersion)

    defaultConfig {
        applicationId =  AndroidSdk.Sample.applicationId
        minSdkVersion(AndroidSdk.minSdkVersion)
        targetSdkVersion(AndroidSdk.targetSdkVersion)
        versionCode = AndroidSdk.Sample.versionCode
        versionName = AndroidSdk.Sample.versionName
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    // Compatibility
    implementation("androidx.appcompat:appcompat:${Libraries.AndroidX.appCompat}")

    // AndroidX
    implementation("androidx.core:core-ktx:${Libraries.AndroidX.coreKtx}")

    // Flat layout widget
    implementation("androidx.constraintlayout:constraintlayout:${Libraries.AndroidX.constraintLayout}")

    // Concurrent coding
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Libraries.coroutinesCore}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Libraries.coroutinesCore}")

    // Library
    implementation(project(":core"))
    //implementation("io.gierla.reactivecomponents:Core:0.0.12")
    kapt(project(":annotationprocessor"))
    //kapt("io.gierla.reactivecomponents:AnnotationProcessor:0.0.12")
}
