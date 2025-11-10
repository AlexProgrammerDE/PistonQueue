plugins {
  `java-library`
  `maven-publish`
  id("net.ltgt.errorprone")
  id("com.github.spotbugs")
  id("org.openrewrite.rewrite")
}

rewrite {
  activeRecipe("org.openrewrite.staticanalysis.CodeCleanup")
  activeRecipe("org.openrewrite.java.migrate.UpgradeToJava21")
  activeRecipe("org.openrewrite.java.recipes.RecipeTestingBestPractices")
  isExportDatatables = true
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {
  implementation("org.jetbrains:annotations:26.0.2-1")

  errorprone("com.google.errorprone:error_prone_core:2.44.0")
  spotbugs("com.github.spotbugs:spotbugs:4.9.8")
  rewrite("org.openrewrite.recipe:rewrite-static-analysis:2.21.0")
  rewrite("org.openrewrite.recipe:rewrite-migrate-java:3.21.1")
  rewrite("org.openrewrite.recipe:rewrite-rewrite:0.15.0")

  compileOnly("org.projectlombok:lombok:1.18.42")
  annotationProcessor("org.projectlombok:lombok:1.18.42")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
  testImplementation("org.mockito:mockito-core:5.20.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
}

tasks {
  processResources {
    inputs.property("version", project.version)
    inputs.property("description", project.description)
    filesMatching(
      listOf(
        "plugin.yml",
        "bungee.yml",
        "velocity-plugin.json",
        "pistonqueue-build-data.properties"
      )
    ) {
      expand(
        mapOf(
          "version" to inputs.properties["version"],
          "description" to inputs.properties["description"],
          "url" to "https://modrinth.com/plugin/pistonqueue",
        )
      )
    }
  }
  test {
    reports.junitXml.required = true
    reports.html.required = true
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).coerceAtLeast(1)
  }
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
  options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
publishing {
  repositories {
    maven("https://repo.codemc.org/repository/${repoName}/") {
      credentials.username = System.getenv("CODEMC_USERNAME")
      credentials.password = System.getenv("CODEMC_PASSWORD")
      name = "codemc"
    }
  }
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name = "PistonQueue"
        description = rootProject.description
        url = "https://github.com/AlexProgrammerDE/PistonQueue"
        organization {
          name = "AlexProgrammerDE"
          url = "https://pistonmaster.net"
        }
        developers {
          developer {
            id = "AlexProgrammerDE"
            timezone = "Europe/Berlin"
            url = "https://pistonmaster.net"
          }
        }
        licenses {
          license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0"
          }
        }
        scm {
          connection = "scm:git:https://github.com/AlexProgrammerDE/PistonQueue.git"
          developerConnection = "scm:git:ssh://git@github.com/AlexProgrammerDE/PistonQueue.git"
          url = "https://github.com/AlexProgrammerDE/PistonQueue"
        }
        ciManagement {
          system = "GitHub Actions"
          url = "https://github.com/AlexProgrammerDE/PistonQueue/actions"
        }
        issueManagement {
          system = "GitHub"
          url = "https://github.com/AlexProgrammerDE/PistonQueue/issues"
        }
      }
    }
  }
}
