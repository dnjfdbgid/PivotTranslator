import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// local.properties에서 API 키 읽기
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.tyua.pivottranslator"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // 릴리즈 서명 설정 (local.properties에서 키스토어 정보 읽기)
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("KEYSTORE_PATH", ""))
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("KEY_PASSWORD", "")
        }
    }

    defaultConfig {
        applicationId = "com.tyua.pivottranslator"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties에서 PivotGate API 키 주입
        buildConfigField(
            "String",
            "PIVOT_GATE_API_KEY",
            "\"${localProperties.getProperty("PIVOT_GATE_API_KEY", "")}\""
        )
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Retrofit2 + OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // Gson
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)


    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// AGP 9 workaround: ART profile compilation generates .dm files that cause
// INSTALL_BASELINE_PROFILE_FAILED when deploying release builds from Android Studio
tasks.configureEach {
    if (name.startsWith("compile") && name.endsWith("ArtProfile")) {
        enabled = false
    }
}

// 릴리즈 APK 파일명: PivotTranslator_release_yyyyMMdd.apk
afterEvaluate {
    tasks.findByName("assembleRelease")?.let { assembleTask ->
        tasks.register<Copy>("copyReleaseApk") {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            from(layout.buildDirectory.dir("outputs/apk/release"))
            include("app-release.apk")
            into(rootProject.layout.buildDirectory.dir("release"))
            rename { "PivotTranslator_release_$date.apk" }
        }
        assembleTask.finalizedBy("copyReleaseApk")
    }
}