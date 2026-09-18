plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.0" apply false
}

allprojects {
    group = "net.modclaim.asc"
    version = "1.0.1"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://maven.enginehub.org/repo/")
    }
}

// Disable empty root project jar
tasks.jar {
    enabled = false
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Convenient task to collect ready-to-upload release jars into a single folder
tasks.register<Copy>("releaseJars") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("shadowJar") })

    into(rootProject.layout.projectDirectory.dir("release"))

    from(project(":asc-loader").tasks.named("shadowJar")) {
        rename { "AdaptiveServerCore-Universal.jar" }
    }
    from(project(":asc-platform-paper-latest").tasks.named("shadowJar")) {
        rename { "AdaptiveServerCore-Paper-1.21.jar" }
    }
    from(project(":asc-platform-paper-legacy").tasks.named("shadowJar")) {
        rename { "AdaptiveServerCore-Paper-Legacy.jar" }
    }
    from(project(":asc-platform-spigot").tasks.named("shadowJar")) {
        rename { "AdaptiveServerCore-Spigot.jar" }
    }
    from(project(":asc-platform-bukkit").tasks.named("shadowJar")) {
        rename { "AdaptiveServerCore-Bukkit.jar" }
    }
}
