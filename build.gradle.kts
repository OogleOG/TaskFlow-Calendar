plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

repositories {
    mavenCentral()
}

val osName = System.getProperty("os.name").lowercase()
val platform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    osName.contains("linux") -> "linux"
    else -> throw GradleException("Unknown OS: $osName")
}

val javafxVersion = "20.0.2"

dependencies {
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
    implementation("org.json:json:20230618")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$platform")
}

application {
    mainClass.set("org.oogle.calender.CalendarApp")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.oogle.calender.CalendarApp"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.joinToString(" ") { it.name }
    }
}

// NEW: Copy all dependencies to build/jpackage-input
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("build/jpackage-input")
}

tasks.register<Copy>("prepareJpackage") {
    dependsOn("build", "copyDependencies")
    from(tasks.jar)
    into("build/jpackage-input")
}

tasks.register<Exec>("createInstaller") {
    dependsOn("prepareJpackage")

    commandLine(
        "jpackage",
        "--input", "build/jpackage-input",
        "--name", "TaskFlowCalendar",
        "--main-jar", "calender.jar",
        "--vendor", "Oogle",
        "--main-class", "org.oogle.calender.CalendarApp",
        "--icon", "calendar-icon.ico",
        "--type", "app-image",
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Djava.library.path=\$APPDIR",
        "--verbose"
    )
}