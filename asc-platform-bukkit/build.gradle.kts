plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":asc-core"))
    implementation(project(":asc-lazysim"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("AdaptiveServerCore-Bukkit")
    archiveClassifier.set("")
}
