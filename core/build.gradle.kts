plugins {
    `java-library`
}

val adventureVersion = "4.17.0"

dependencies {
    api("com.zaxxer:HikariCP:4.0.3")
    api("org.yaml:snakeyaml:2.2")
    api("org.xerial:sqlite-jdbc:3.46.0.0")
    api("com.mysql:mysql-connector-j:8.4.0") {
        exclude(group = "com.google.protobuf")
    }

    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
}
