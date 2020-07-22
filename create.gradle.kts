apply(plugin = "org.jetbrains.dokka")
apply(plugin = "maven-publish")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId = LibConfig.groupId
                artifactId = extra.get("artifactId") as String
                version = LibConfig.version

                from(components["java"])

                artifact(sourcesJar)
                artifact(javadocJar)
            }
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("dokka"))
}