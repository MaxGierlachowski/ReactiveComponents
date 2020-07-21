plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Local libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    // Concurrent coding
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Libraries.coroutinesCore}")

    // Library
    api(project(":library"))

    // Available annotations
    api(project(":annotations"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}