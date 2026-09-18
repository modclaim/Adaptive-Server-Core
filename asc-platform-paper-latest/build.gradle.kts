plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":asc-core"))
    implementation(project(":asc-lazysim"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("AdaptiveServerCore-Paper-1.21")
    archiveClassifier.set("")
}
