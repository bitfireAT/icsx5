buildscript {
    val aboutLibsVersion by extra("10.7.0")
    val agpVersion = "8.2.1"
    val kotlinVersion = "1.9.22"
    val kspVersion = "1.0.16"

    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:$agpVersion")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath ("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:$aboutLibsVersion")
        classpath ("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${kotlinVersion}-${kspVersion}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
