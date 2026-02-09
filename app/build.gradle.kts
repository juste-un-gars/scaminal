import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Chargement des credentials de signature depuis keystore.properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.scaminal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scaminal"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile", ""))
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("int", "DEFAULT_SCAN_TIMEOUT", "200")
            buildConfigField("int", "DEFAULT_PORT_TIMEOUT", "200")
            buildConfigField("int", "DEFAULT_SSH_TIMEOUT", "10000")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            buildConfigField("int", "DEFAULT_SCAN_TIMEOUT", "200")
            buildConfigField("int", "DEFAULT_PORT_TIMEOUT", "200")
            buildConfigField("int", "DEFAULT_SSH_TIMEOUT", "10000")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Logging
    implementation(libs.timber)

    // SSH
    implementation(libs.jsch)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
