import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val jvmVersion = libs.versions.jvm.get().toInt()

// Shared configuration for all library subprojects.
// Each submodule's build.gradle.kts only needs to declare its own dependencies.
subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    group = "edu.jhu.cobra"
    version = "0.1.2"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    configure<KotlinJvmProjectExtension> {
        explicitApi()
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(jvmVersion))
        }
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    // kotlin("test") resolves to its JUnit 5 variant because useJUnitPlatform is set.
    dependencies {
        "testImplementation"(kotlin("test"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") { from(components["java"]) }
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
    }
}
