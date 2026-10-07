plugins {
    id("com.gradleup.shadow") version "8.3.5" apply false
}

allprojects {
    group = "dev.apexban"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
    }
}

tasks.register<Copy>("dist") {
    group = "build"
    description = "Collects the Bukkit, BungeeCord and Velocity jars into build/dist."
    val platformModules = listOf("bukkit", "bungee", "velocity")
    dependsOn(platformModules.map { ":$it:shadowJar" })
    platformModules.forEach { from(project(":$it").layout.buildDirectory.dir("libs")) }
    include("ApexBan-*.jar")
    exclude("*-slim.jar")
    into(layout.buildDirectory.dir("dist"))
}
