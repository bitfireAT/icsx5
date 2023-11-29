import com.mikepenz.aboutlibraries.plugin.DuplicateMode

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 34

    namespace = "at.bitfire.icsdroid"

    defaultConfig {
        applicationId = "at.bitfire.icsdroid"
        minSdk = 21
        targetSdk = 33

        versionCode = 73
        versionName = "2.2-beta.1"

        setProperty("archivesBaseName", "icsx5-$versionCode-$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
        dataBinding = true
        viewBinding = true
    }

    composeOptions {
        // Keep in sync with Kotlin version: https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {}
        create("gplay") {}
    }

    signingConfigs {
        create("bitfire") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE") ?: "/dev/null")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("bitfire")
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
    val aboutLibsVersion: String by rootProject.extra
    val composeBomVersion = "2023.08.00"   // https://developer.android.com/jetpack/compose/bom
    val okhttp = "5.0.0-alpha.11"
    val room = "2.6.0"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.github.bitfireAT:cert4android:3817e62d9f173d8f8b800d24769f42cb205f560e")
    implementation("com.github.bitfireAT:ical4android:b682476")

    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.android.material:material:1.10.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:${composeBomVersion}")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material:material")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
    implementation("com.google.accompanist:accompanist-themeadapter-material:0.32.0")
    implementation("io.github.vanpra.compose-material-dialogs:color:0.9.0")

    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("com.mikepenz:aboutlibraries-compose:${aboutLibsVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okhttp}")
    implementation("com.squareup.okhttp3:okhttp-brotli:${okhttp}")
    implementation("com.squareup.okhttp3:okhttp-coroutines:${okhttp}")
    implementation("joda-time:joda-time:2.12.5")

    // latest commons that don"t require Java 8
    //noinspection GradleDependency
    implementation("commons-io:commons-io:2.6")
    //noinspection GradleDependency
    implementation("org.apache.commons:commons-lang3:3.8.1")

    // Room Database
    implementation("androidx.room:room-ktx:${room}")
    ksp("androidx.room:room-compiler:${room}")

    // for tests
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:${okhttp}")
    androidTestImplementation("androidx.work:work-testing:2.8.1")

    testImplementation("junit:junit:4.13.2")
}

aboutLibraries {
    duplicationMode = DuplicateMode.MERGE
    includePlatform = false
}
