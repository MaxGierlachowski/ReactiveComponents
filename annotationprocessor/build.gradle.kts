import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-library`
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("${buildDir.absolutePath}/tmp/kapt/main/kotlinGenerated/")
    }
}

dependencies {
    // Local libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    // Auto register annotation
    implementation("com.google.auto.service:auto-service:${Libraries.googleAutoService}")
    kapt("com.google.auto.service:auto-service:${Libraries.googleAutoService}")

    // Creating *.kt files
    implementation("com.squareup:kotlinpoet:${Libraries.kotlinPoet}")

    // Concurrent coding
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Libraries.coroutinesCore}")

    // Available Annotations and library classes
    implementation(project(":core"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

extra.apply{
    set("artifactId", "AnnotationProcessor")
    set("description", "Annotation processor used to make the use of reactive components easier.")
}

apply(from = "${rootDir}/create.gradle.kts")
apply(from = "${rootDir}/publish.gradle.kts")