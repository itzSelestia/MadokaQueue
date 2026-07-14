plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // shaded into our jar, so the server does not need it installed as a plugin
    implementation("com.github.retrooper:packetevents-spigot:2.13.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        // packetevents requires relocation when shaded, otherwise it clashes with any other
        // plugin that bundles its own copy
        relocate("com.github.retrooper.packetevents", "dev.yae.madokaQueue.shaded.packetevents.api")
        relocate("io.github.retrooper.packetevents", "dev.yae.madokaQueue.shaded.packetevents.impl")
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
