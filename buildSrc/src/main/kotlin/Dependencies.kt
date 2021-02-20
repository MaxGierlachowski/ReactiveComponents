const val kotlinVersion = "1.4.20"

object BuildPlugins {
    const val androidGradle = "4.1.2"
    const val dokka = "1.4.20"
    const val mavenPublish = "0.13.0"
}

object AndroidSdk {
    const val compileSdkVersion = 29
    const val minSdkVersion = 16
    const val targetSdkVersion = 29

    object Sample {
        const val applicationId = "io.gierla.rcsample"
        const val versionCode = 1
        const val versionName = "1.0.0"
    }
}

object Libraries {
    const val coroutinesCore = "1.3.6"
    const val kotlinPoet = "1.5.0"

    const val googleAutoService = "1.0-rc7"

    object AndroidX {
        const val appCompat = "1.1.0"
        const val coreKtx = "1.3.0"
        const val constraintLayout = "1.1.3"
    }
}

object LibConfig {
    const val groupId = "io.gierla.reactivecomponents"
    const val version = "0.0.21"
}