plugins {
    application
    pmd
    id("com.diffplug.spotless") version "8.10.2"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "App"
}

spotless {
    java {
        googleJavaFormat()
    }
}

pmd {
    toolVersion = "7.27.0"
}
