plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

intellij {
    version.set("2023.1.4")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf(/* 依赖的其他插件 */))
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }
} 