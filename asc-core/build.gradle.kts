dependencies {
    api(project(":asc-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")
    compileOnly("me.clip:placeholderapi:2.11.5")
}
