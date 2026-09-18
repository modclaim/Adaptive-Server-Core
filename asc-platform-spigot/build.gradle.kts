plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":asc-core"))
    implementation(project(":asc-lazysim"))
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("AdaptiveServerCore-Spigot")
    archiveClassifier.set("")
}
