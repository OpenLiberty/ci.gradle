pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri(file("$rootDir/../../plugin-test-repository/"))
        }
    }
    val lgpVersion: String by settings
    plugins {
        id("io.openliberty.tools.gradle.Liberty") version "${lgpVersion}"
    }
    
}