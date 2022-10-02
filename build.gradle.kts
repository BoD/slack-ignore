import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
    id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j", "slf4j-api", "_")
    implementation("org.slf4j", "slf4j-simple", "_")

    implementation(KotlinX.Coroutines.jdk8)

    implementation(Square.OkHttp3.okHttp)
    implementation(Square.Retrofit2.retrofit)
    implementation(Square.Retrofit2.Converter.moshi)
    implementation(Square.moshi)
    kapt(Square.Moshi.kotlinCodegen)

    implementation(KotlinX.cli)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.jraf.slackignore.MainKt")
}

docker {
    javaApplication {
        maintainer.set("BoD <BoD@JRAF.org>")
        ports.set(emptyList())
        images.add("bodlulu/${rootProject.name}:latest")
    }
    registryCredentials {
        username.set(System.getenv("DOCKER_USERNAME"))
        password.set(System.getenv("DOCKER_PASSWORD"))
    }
}

tasks.withType<DockerBuildImage> {
    platform.set("linux/amd64")
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew distZip` to create a zip distribution
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
