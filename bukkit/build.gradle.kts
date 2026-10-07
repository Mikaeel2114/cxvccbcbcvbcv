plugins {
    java
    id("com.gradleup.shadow")
}

val adventureVersion = "4.17.0"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":core"))
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-nop:2.0.13")

    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version.toString()))
    }
}

tasks.jar {
    archiveClassifier.set("slim")
}

tasks.shadowJar {
    archiveBaseName.set("ApexBan-Bukkit")
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.zaxxer.hikari", "dev.apexban.libs.hikari")
    relocate("org.yaml.snakeyaml", "dev.apexban.libs.snakeyaml")
    relocate("org.slf4j", "dev.apexban.libs.slf4j")
    relocate("net.kyori", "dev.apexban.libs.kyori")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/LICENSE*", "META-INF/NOTICE*")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
