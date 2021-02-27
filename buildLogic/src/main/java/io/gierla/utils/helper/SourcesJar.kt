package io.gierla.utils.helper

import org.gradle.jvm.tasks.Jar

open class SourcesJar : Jar() {
    init {
        archiveClassifier.set("sources")
        from(project.sourceSets().getByName("main").allSource)
    }
}

