plugins {
  `java-library`
  `maven-publish`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {
  implementation("org.jetbrains:annotations:26.0.2")

  compileOnly("org.projectlombok:lombok:1.18.38")
  annotationProcessor("org.projectlombok:lombok:1.18.38")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
  testImplementation("org.mockito:mockito-core:5.18.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

tasks {
  processResources {
    filesMatching(
      listOf(
        "plugin.yml",
        "bungee.yml",
        "velocity-plugin.json",
        "pistonqueue-build-data.properties"
      )
    ) {
      expand(
        "version" to project.version,
        "description" to project.description,
        "url" to "https://modrinth.com/plugin/pistonqueue",
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
