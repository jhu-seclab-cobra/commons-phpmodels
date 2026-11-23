import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val minJavaProperty = providers.gradleProperty("minJava").get()
val minJava =
    minJavaProperty.toIntOrNull()?.takeIf { it >= 8 }
        ?: throw GradleException("minJava must be a Java version of 8 or later, got '$minJavaProperty'.")
val targetJavaVersion = JavaVersion.toVersion(minJava)
val runningJavaVersion = JavaVersion.current()
if (runningJavaVersion < targetJavaVersion) {
    throw GradleException(
        "minJava=$minJava requires JDK $minJava or later to run Gradle. " +
            "Gradle is running on JDK ${runningJavaVersion.majorVersion}.",
    )
}

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
    version = "0.12.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    configure<KotlinJvmProjectExtension> {
        explicitApi()
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
            freeCompilerArgs.add("-Xjdk-release=$minJava")
        }
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = targetJavaVersion
        targetCompatibility = targetJavaVersion
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(minJava)
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
        version.set("1.8.0")
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
