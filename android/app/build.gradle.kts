plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.ownvoice"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ownvoice.app"
        minSdk = 24
        targetSdk = 36
        // Keep these in sync with the product version used by the desktop
        // bridge, the cloud relay and the web download routes (see
        // core/bridge_server.py, server/cloud_relay.py). The published
        // v3.0.0 APKs were all built as code 17 / "2.5.6", so the next release
        // uses code 18 / "3.0.0".
        versionCode = 18
        versionName = "3.0.0"
    }

    signingConfigs {
        create("release") {
            // Signing credentials are NEVER hardcoded or committed.
            // Provide them through environment variables (CI) or `keystore.properties`
            // (local, git-ignored) — see README / keystore.properties.example.
            val props = java.util.Properties().apply {
                val f = rootProject.file("keystore.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            fun cred(env: String, prop: String): String? =
                System.getenv(env) ?: props.getProperty(prop)

            storeFile = file(cred("OWNVOICE_KEYSTORE_FILE", "storeFile") ?: "release.keystore")
            storePassword = cred("OWNVOICE_KEYSTORE_PASSWORD", "storePassword")
            keyAlias = cred("OWNVOICE_KEY_ALIAS", "keyAlias") ?: "ownvoice"
            keyPassword = cred("OWNVOICE_KEY_PASSWORD", "keyPassword") ?: storePassword
        }
    }

    buildTypes {
        release {
            // R8 shrinks + obfuscates the release APK (the ProGuard rules it
            // uses live in android/app/proguard-rules.pro) and strips unused
            // resources so the shipped APK stays small.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Debug builds use Gradle's automatic debug keystore — the release
            // signing key must never be needed (or leaked) for local debug work.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Network & Icons
  implementation(libs.okhttp)
  implementation(libs.androidx.compose.material.icons)

  // QR Code Scanning
  implementation("com.journeyapps:zxing-android-embedded:4.3.0")
  implementation("com.google.zxing:core:3.5.3")
}


// Fail loudly (instead of producing an unsigned/incorrectly signed artifact)
// when release signing credentials have not been supplied.
tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }.configureEach {
    doFirst {
        val cfg = signingConfigs.getByName("release")
        if (cfg.storePassword.isNullOrBlank() || cfg.storeFile?.exists() != true) {
            throw GradleException(
                "Release signing credentials are missing. Set OWNVOICE_KEYSTORE_FILE / " +
                    "OWNVOICE_KEYSTORE_PASSWORD (and optionally OWNVOICE_KEY_ALIAS / " +
                    "OWNVOICE_KEY_PASSWORD) or create android/keystore.properties. " +
                    "See android/keystore.properties.example."
            )
        }
    }
}
