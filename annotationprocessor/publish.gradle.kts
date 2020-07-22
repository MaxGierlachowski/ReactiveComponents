apply(plugin = "maven-publish")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId =  LibConfig.groupId
                artifactId = "annotationprocessor"
                version = LibConfig.version

                from(components["java"])

                artifact(sourcesJar)
            }
        }
    }
}


val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}
