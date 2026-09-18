plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":asc-api"))
    implementation(project(":asc-core"))
    implementation(project(":asc-lazysim"))
    implementation(project(":asc-platform-paper-latest"))
    implementation(project(":asc-platform-paper-legacy"))
    implementation(project(":asc-platform-spigot"))
    implementation(project(":asc-platform-bukkit"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("AdaptiveServerCore-Universal")
    archiveClassifier.set("")
}
