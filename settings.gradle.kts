buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.martmists.com/releases/")
    }
    dependencies {
        classpath("com.martmists.commons:commons-gradle:1.0.4")
    }
}

rootProject.name = "custom_language"
for (f in rootProject.projectDir.listFiles()) {
    if (f.isDirectory && f.name.startsWith("part_")) {
        include(":${f.name}")
    }
}
