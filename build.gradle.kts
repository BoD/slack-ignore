plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
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
    implementation(Square.Retrofit2.retrofit)
    implementation(Square.Retrofit2.Converter.moshi)
    implementation(Square.moshi)
    kapt(Square.Moshi.kotlinCodegen)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.jraf.slackignore.MainKt")
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "7.3.1"
    }

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    register("stage") {
        dependsOn(":installDist")
    }
}

// Run `./gradlew refreshVersions` to update dependencies
// Run `./gradlew distZip` to create a zip distribution
