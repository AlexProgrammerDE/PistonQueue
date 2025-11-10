plugins {
    base
    id("org.openrewrite.rewrite") version "latest.release"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "net.pistonmaster"
    version = property("maven_version")!!
    description = "Best queue plugin out there!"
}

dependencies {
    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
}

rewrite {
    activeRecipe("org.openrewrite.java.ShortenFullyQualifiedTypeReferences")
}

tasks.register("outputVersion") {
    doLast {
        println(project.version)
    }
}
