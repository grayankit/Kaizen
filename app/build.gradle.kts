plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePath = System.getenv("KEYSTORE_FILE")
val releaseVersion = releaseVersion()

android {
    namespace = "me.ankitgrai.kaizen"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "me.ankitgrai.kaizen"
        minSdk = 29
        targetSdk = 37
        versionCode = releaseVersion.code
        versionName = releaseVersion.name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (!keystorePath.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = requiredEnv("KEYSTORE_PASSWORD")
                keyAlias = requiredEnv("KEY_ALIAS")
                keyPassword = requiredEnv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    //Icons
    implementation(libs.androidx.compose.material.icons.extended)
    //Navigation
    implementation(libs.androidx.navigation.compose)
    //lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
}

data class ReleaseVersion(val name: String, val code: Int)

fun releaseVersion(): ReleaseVersion {
    val tag = System.getenv("RELEASE_TAG")?.takeIf { it.isNotBlank() }
        ?: return ReleaseVersion(name = "1.0", code = 1)
    val match = Regex("""^v(\d+)\.(\d+)\.(\d+)$""").matchEntire(tag)
        ?: error("RELEASE_TAG must be vMAJOR.MINOR.PATCH, was '$tag'")
    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].toInt()
    require(minor < 100 && patch < 100) {
        "Minor and patch must be below 100 so versionCode stays unique. Tag was '$tag'."
    }
    return ReleaseVersion(
        name = "$major.$minor.$patch",
        code = major * 10_000 + minor * 100 + patch
    )
}

fun requiredEnv(name: String): String {
    return System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: error("$name is required to sign a release")
}