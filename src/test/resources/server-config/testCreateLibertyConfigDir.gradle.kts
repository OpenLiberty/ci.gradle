group = "liberty.gradle"
version = "1"

val runtimeVersion: String by project
val runtimeGroup: String by project
val kernelArtifactId: String by project

plugins {
    java
    id("io.openliberty.tools.gradle.Liberty")
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(7)
}

repositories {
    mavenCentral()
    maven {
        name = "liberty-starter-maven-repo"
        url = uri("https://liberty-starter.wasdev.developer.ibm.com/start/api/v1/repo")
    }
}

val jdbcLib by configurations.creating
val serverName by extra { "LibertyProjectServer" }

dependencies {
    testImplementation("junit:junit:4.13.1")
    jdbcLib("org.postgresql:postgresql:42.3.8")
    libertyRuntime("${runtimeGroup}:${kernelArtifactId}:${runtimeVersion}")
}

tasks {
    val integrationTest by registering(Test::class) {
        group = "Verification"
        description = "Runs the integration tests."
        reports.html.outputLocation.set(file("$buildDir/reports/it"))
        reports.junitXml.outputLocation.set(file("$buildDir/test-results/it"))
        include("**/it/**")
        exclude("**/unit/**")
    }

    val copyJdbcLib by registering(Copy::class) {
        from(jdbcLib)
        into("$buildDir/wlp/usr/shared/resources")
        include("*.jar")
    }

    integrationTest {
        dependsOn(libertyCreate)
    }

    libertyCreate {
        liberty.server.name = serverName
        liberty.server.configDirectory = file("${project.projectDir}/src/test/resources") 
        dependsOn(copyJdbcLib)
        outputs.upToDateWhen { false }
    }

    copyJdbcLib {
        dependsOn(installLiberty)
    }

    check {
        dependsOn(integrationTest)
    }
}

