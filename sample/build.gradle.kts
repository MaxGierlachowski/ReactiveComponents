import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    // Compatibility
    implementation("androidx.appcompat:appcompat:${Libraries.AndroidX.appCompat}")

    // AndroidX
    implementation("androidx.core:core-ktx:${Libraries.AndroidX.coreKtx}")

    // Flat layout widget
    implementation("androidx.constraintlayout:constraintlayout:${Libraries.AndroidX.constraintLayout}")

    // Efficient lists
    implementation("androidx.recyclerview:recyclerview:${Libraries.AndroidX.recyclerView}")

    // Concurrent coding
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Libraries.coroutinesCore}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Libraries.coroutinesCore}")

    // Library
    implementation(project(":views"))
    kapt(project(":annotationprocessors"))
}
