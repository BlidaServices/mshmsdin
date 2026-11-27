import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.mshmsdin.captionsearch"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.mshmsdin.captionsearch"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        val storePath = localProperties.getProperty("storeFile")
        if (!storePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(storePath)
                storePassword = localProperties.getProperty("storePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

@Suppress("UNCHECKED_CAST")
val cachedVersions: Map<String, String> = run {
    val file = rootProject.file("gradle/cachedVersions.json")
    if (file.exists()) {
        groovy.json.JsonSlurper().parse(file) as Map<String, String>
    } else emptyMap()
}

configurations.all {
    resolutionStrategy.eachDependency {
        val forced = cachedVersions["${requested.group}:${requested.name}"]
        if (forced != null && requested.version != forced) {
            useVersion(forced)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.webkit)
    implementation(files("libs/sqlite-jdbc-3.41.2.2.jar"))
}
