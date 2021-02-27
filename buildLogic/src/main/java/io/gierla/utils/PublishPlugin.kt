package io.gierla.utils

import io.gierla.utils.helper.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin

open class PublishPlugin : Plugin<Project> {

    companion object {
        const val PUBLICATION_NAME = "maven"
    }

    override fun apply(project: Project) {
        project.configurePlugins()
        project.configureCoordinates()
        project.configureDokka()
        project.afterEvaluate {
            project.configurePublishing()
            project.configureSigning()
        }
    }

    private fun Project.configurePlugins() {
        plugins.apply(MavenPublishPlugin::class.java)
        plugins.apply(SigningPlugin::class.java)
    }

     private fun Project.configureCoordinates() {
         group = strProp("GROUP")
         version = strProp("VERSION_NAME")
     }

     private fun Project.configureSigning() = signing().run {
         sign(publishing().publications[PUBLICATION_NAME])
     }

     private fun Project.configureDokka() {
         project.plugins.withId("org.jetbrains.kotlin.jvm") {
             plugins.apply(DokkaPlugin::class.java)
         }
     }

    private fun Project.configurePublishing() = publishing().run {
        publications {
            create(PUBLICATION_NAME, MavenPublication::class.java) {
                artifactId = strProp("POM_ARTIFACT_ID")

                from(components.findByName("java"))
                artifact(tasks.register("sourcesJar", SourcesJar::class.java))
                artifact(tasks.register("javadocsJar", JavadocsJar::class.java))

                pom {
                    name.set(strProp("POM_NAME"))
                    description.set(strProp("POM_DESCRIPTION"))
                    url.set(strProp("POM_URL"))
                    licenses {
                        license {
                            name.set(strProp("POM_LICENCE_NAME"))
                            url.set(strProp("POM_LICENCE_URL"))
                        }
                    }
                    developers {
                        developer {
                            id.set(strProp("POM_DEVELOPER_ID"))
                            name.set(strProp("POM_DEVELOPER_NAME"))
                            email.set(strProp("POM_DEVELOPER_MAIL"))
                            url.set(strProp("POM_DEVELOPER_URL"))
                        }
                    }
                    scm {
                        connection.set(strProp("POM_SCM_CONNECTION"))
                        developerConnection.set(strProp("POM_SCM_DEV_CONNECTION"))
                        url.set(strProp("POM_URL"))
                    }
                }

                repositories {
                    maven {
                        name = "mavenCentral"
                        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        credentials {
                            username = strProp("STAGE_USER")
                            password = strProp("STAGE_PASSWORD")
                        }
                        //credentials(PasswordCredentials::class.java)
                        /*name = "TestRepo"
                        url = uri("file://${buildDir}/repo")*/
                    }
                }
            }
        }
    }

}
