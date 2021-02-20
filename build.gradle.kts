plugins {
    kotlin("android") version kotlinVersion apply false
    kotlin("kapt") version kotlinVersion apply false
    kotlin("jvm") version kotlinVersion apply false

    id("org.jetbrains.dokka") version BuildPlugins.dokka

    id("com.android.application") version BuildPlugins.androidGradle apply false
    id("com.vanniktech.maven.publish") version BuildPlugins.mavenPublish apply false
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}