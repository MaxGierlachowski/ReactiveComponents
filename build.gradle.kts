buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${BuildPlugins.androidGradle}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${BuildPlugins.dokka}")
    }
}

allprojects {
    repositories {
        google()
        jcenter()

        maven {
            url = uri("https://dl.bintray.com/maxgierlachowski/ReactiveComponents")
        }
    }
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}
