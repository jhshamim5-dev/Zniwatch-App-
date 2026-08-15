import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.blackpage.hmadho"
    minSdk = 24
    targetSdk = 36
    versionCode = 4
    versionName = "1.0.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val rootKeystore = file("${rootDir}/debug.keystore")
  if (!rootKeystore.exists() || rootKeystore.length() == 0L) {
    val b64File = file("${rootDir}/debug.keystore.base64")
    val b64String = if (b64File.exists() && b64File.length() > 0L) {
      b64File.readText().trim()
    } else {
      "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"
    }
    try {
      val decoded = Base64.getDecoder().decode(b64String)
      rootKeystore.writeBytes(decoded)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  signingConfigs {
    create("release") {
      val envPath = System.getenv("KEYSTORE_PATH")
      val keystoreFile = if (!envPath.isNullOrEmpty() && file(envPath).exists()) file(envPath) else file("${rootDir}/debug.keystore")
      storeFile = keystoreFile
      storePassword = System.getenv("STORE_PASSWORD").takeIf { !it.isNullOrEmpty() } ?: "android"
      keyAlias = System.getenv("KEY_ALIAS").takeIf { !it.isNullOrEmpty() } ?: "androiddebugkey"
      keyPassword = System.getenv("KEY_PASSWORD").takeIf { !it.isNullOrEmpty() } ?: "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.coil.compose)
  implementation(libs.jsoup)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
