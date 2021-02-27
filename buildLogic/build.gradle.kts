plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    jcenter()
}

dependencies {
    // Local libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Gradle Api
    api(gradleApi())

    val kotlinVersion = "1.4.30"

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Documentation
    compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")
}

gradlePlugin {
    plugins {
        create("PublishPlugin") {
            id = "publishPlugin"
            implementationClass = "io.gierla.utils.PublishPlugin"
        }
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}