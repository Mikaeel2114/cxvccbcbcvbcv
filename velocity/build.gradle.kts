plugins {
    java
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.jar {
    archiveClassifier.set("slim")
}

tasks.shadowJar {
    archiveBaseName.set("ApexBan-Velocity")
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.zaxxer.hikari", "dev.apexban.libs.hikari")
    relocate("org.yaml.snakeyaml", "dev.apexban.libs.snakeyaml")
    exclude("org/slf4j/**")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/LICENSE*", "META-INF/NOTICE*")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
