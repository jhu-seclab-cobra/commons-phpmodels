plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "commons-phpmodels"

include("jhu-seclab-cobra-commons-phpmodels")
project(":jhu-seclab-cobra-commons-phpmodels").projectDir = file("commons-phpmodels")
