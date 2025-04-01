plugins {
    vikbot.compile
    alias(libs.plugins.kotlin.serialization)

    vikbot.repos
}

dependencies {
    implementation(projects.api)
    implementation(libs.discord.jda)
    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.kotlin.reflect)
    implementation("commons-codec:commons-codec:1.18.0")

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.cli)

    implementation(libs.kotlinx.coroutines.core)


    implementation(libs.okio) // explicitly implement okio
    implementation(libs.json.json)

    // Logger stuff
    implementation(libs.slf4j)
    implementation(libs.slf4k)
    implementation(libs.logback)
    implementation(libs.google.guava)

    implementation(libs.graal.polyglot)
    implementation(libs.graal.js)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    compileOnly(libs.google.findbugs) // Only used in library, use JetBrains nullability instead (in case something has to be written in java)
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar{
        manifest {
            attributes (
                "Main-Class" to "dev.kosmx.discorducky.Launch"
            )
        }
        dependsOn("copyDependencies")
    }

    register<Copy>("copyDependencies") {
        from(configurations.runtimeClasspath).into("${layout.buildDirectory.asFile.get()}/dependencies")
    }

    /*
    shadowJar {
        archiveFileName = "bot-all.jar"
    }*/
}
