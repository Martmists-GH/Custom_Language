import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.martmists.commons.*

plugins {
    java
    kotlin("jvm") version "1.7.10" apply false
    id("com.github.ben-manes.versions") version "0.42.0"
}

group = "com.martmists"
version = "1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
    buildDir = file("${rootProject.buildDir.absolutePath}/${project.name}")

    apply(plugin="org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        martmists()
    }

    dependencies {
        implementation("com.martmists:kotpack:1.0.3")
        testImplementation(kotlin("test"))
    }
}

tasks {
    withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isStable(currentVersion) && !isStable(candidate.version)
        }
    }
}
