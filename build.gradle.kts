plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("io.papermc.paperweight.userdev") version "1.7.4"
}

group = "me.literka"
version = "1.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.ProjectKorra:ProjectKorra:v1.11.2")
    implementation("com.github.retrooper:packetevents-spigot:2.7.0")
}