import com.jfrog.bintray.gradle.BintrayPlugin
import java.util.*

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:${BuildPlugins.bintray}")
    }
}
plugins.apply(BintrayPlugin::class)

val bintrayUser: String = project.findProperty("bintrayUser").toString()
val bintrayApiKey: String = project.findProperty("bintrayKey").toString()

configure<com.jfrog.bintray.gradle.BintrayExtension> {
    val properties = Properties()
    val propertiesFile = project.rootProject.file("${rootDir}/local.properties")
    if (propertiesFile.exists()) {
        properties.load(java.io.FileInputStream("${rootDir}/local.properties"))
    }

    user = properties.getProperty("bintray.user")
    key = properties.getProperty("bintray.apikey")

    setPublications("maven")

    override = true

    pkg.apply {
        repo = "ReactiveComponents"
        name = extra.get("artifactId") as String
        description = extra.get("description") as String
        publicDownloadNumbers = true
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/MaxGierlachowski/ReactiveComponents"
        version.apply {
            name = LibConfig.version
            desc = "Version ${LibConfig.version}"
            vcsTag = LibConfig.version
        }
    }
}