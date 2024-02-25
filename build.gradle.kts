// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.aboutLibs) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
}
