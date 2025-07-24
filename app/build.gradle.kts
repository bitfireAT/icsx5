import com.mikepenz.aboutlibraries.plugin.DuplicateMode

plugins {
    alias(libs.plugins.aboutLibs)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = 35

    namespace = "at.bitfire.icsdroid"

    defaultConfig {
        applicationId = "at.bitfire.icsdroid"
        minSdk = 23
        targetSdk = 35

        versionCode = 87
        versionName = "2.3.0"

        setProperty("archivesBaseName", "icsx5-$versionCode-$versionName")

        testInstrumentationRunner = "at.bitfire.icsdroid.HiltTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        buildConfig = true
        compose = true
        dataBinding = true
        viewBinding = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {}
        create("gplay") {}
    }

    signingConfigs {
        create("bitfire_apk") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE") ?: "/dev/null")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
        create("bitfire_aab") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE") ?: "/dev/null")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
            keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            productFlavors.getByName("standard").signingConfig = signingConfigs.getByName("bitfire_apk")
            productFlavors.getByName("gplay").signingConfig = signingConfigs.getByName("bitfire_aab")
        }
    }

    lint {
        disable.addAll(
            listOf("ExtraTranslation", "MissingTranslation", "InvalidPackage", "OnClick")
        )
    }

    packaging {
        resources {
            excludes += "META-INF/*.md"
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

configurations {
    configureEach {
        // exclude modules which are in conflict with system libraries
        exclude(module = "commons-logging")
        exclude(group = "org.json", module = "json")

        // Groovy requires SDK 26+, and it"s not required, so exclude it
        exclude(group = "org.codehaus.groovy")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.bitfire.cert4android)
    implementation(libs.bitfire.synctools)

    implementation(libs.compose.dialogs.color)
    implementation(libs.compose.dialogs.core)

    // Hilt
    implementation(libs.hilt.android.base)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime)

    // Jetpack Compose
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIconsExtended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.toolingPreview)
    implementation(libs.compose.runtime.livedata)

    implementation(libs.aboutLibs.compose)
    implementation(libs.jodaTime)

    implementation(libs.okhttp.base)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.coroutines)

    // Room Database
    implementation(libs.room.base)
    ksp(libs.room.compiler)

    // for tests
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)

    testImplementation(libs.junit)
}

aboutLibraries {
    library.duplicationMode = DuplicateMode.MERGE
    collect.includePlatform = false
}
