plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
}

dependencies {


    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.13")
    compileOnly("com.github.TechFortress:GriefPrevention:16.18.7")

}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}



tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
