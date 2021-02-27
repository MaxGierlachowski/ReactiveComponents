package io.gierla.utils.helper

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask

internal fun Project.sourceSets(): SourceSetContainer {
    val javaPlugin = project.convention.getPlugin<JavaPluginConvention>()
    return javaPlugin.sourceSets
}

internal fun Project.publishing(): PublishingExtension {
    return extensions.getByType()
}

internal fun Project.signing(): SigningExtension {
    return extensions.getByType()
}

internal fun Project.findDokkaTask(): DokkaTask {
    val tasks = project.tasks.withType<DokkaTask>()
    return if (tasks.size == 1) {
        tasks.first()
    } else {
        tasks.findByName("dokkaHtml") ?: tasks.getByName("dokka")
    }
}

internal fun Project.strProp(name: String): String = property(name) as String