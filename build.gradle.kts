import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
    id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"


kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j", "slf4j-api", "_")
    implementation("org.slf4j", "slf4j-simple", "_")

    implementation(KotlinX.coroutines.jdk9)

    implementation(Square.okHttp3.okHttp)
    implementation(Square.retrofit2.retrofit)
    implementation(Square.retrofit2.converter.moshi)
    implementation(Square.moshi)
    kapt(Square.moshi.kotlinCodegen)

    implementation(KotlinX.cli)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.jraf.slackignore.MainKt")
}

docker {
    javaApplication {
        // Use OpenJ9 instead of the default one
        baseImage.set("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.18_10_openj9-0.36.1")
        maintainer.set("BoD <BoD@JRAF.org>")
        ports.set(emptyList())
        images.add("bodlulu/${rootProject.name}:latest")
        jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
    }
    registryCredentials {
        username.set(System.getenv("DOCKER_USERNAME"))
        password.set(System.getenv("DOCKER_PASSWORD"))
    }
}

tasks.withType<DockerBuildImage> {
    platform.set("linux/amd64")
}

tasks.withType<Dockerfile> {
    // Move the COPY instructions to the end
    // See https://github.com/bmuschko/gradle-docker-plugin/issues/1093
    instructions.set(
        instructions.get().sortedBy { instruction ->
            if (instruction.keyword == Dockerfile.CopyFileInstruction.KEYWORD) 1 else 0
        }
    )
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew distZip` to create a zip distribution
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
