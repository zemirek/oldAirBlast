plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.7.4"
}

group = "me.literka"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    implementation("com.github.ProjectKorra:ProjectKorra:v1.11.2")
}