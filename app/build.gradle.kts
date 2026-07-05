import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun String.escapeBuildConfigString(): String = replace("\\", "\\\\").replace("\"", "\\\"")

fun configValue(propertyName: String, environmentName: String, defaultValue: String = ""): String {
    return localProperties.getProperty(propertyName)
        ?: providers.environmentVariable(environmentName).orNull
        ?: defaultValue
}

fun tmdbProxyImageBase(apiBaseUrl: String): String {
    val root = apiBaseUrl.trimEnd('/').removeSuffix("/3")
    return "$root/image/t/p/"
}

android {
    namespace = "com.nenah.cinetracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.nenah.cinetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val tmdbToken = configValue("tmdb.api.token", "TMDB_READ_ACCESS_TOKEN")
        val defaultTmdbApiBaseUrl = if (tmdbToken.isBlank()) {
            "http://10.0.2.2:8080/3/"
        } else {
            "https://api.themoviedb.org/3/"
        }
        val tmdbApiBaseUrl = configValue("tmdb.api.baseUrl", "TMDB_API_BASE_URL", defaultTmdbApiBaseUrl)
        val defaultTmdbImageBaseUrl = if (tmdbApiBaseUrl.startsWith("https://api.themoviedb.org")) {
            "https://image.tmdb.org/t/p/"
        } else {
            tmdbProxyImageBase(tmdbApiBaseUrl)
        }
        val tmdbImageBaseUrl = configValue("tmdb.image.baseUrl", "TMDB_IMAGE_BASE_URL", defaultTmdbImageBaseUrl)
        val poiskKinoApiKey = configValue("poiskkino.api.key", "POISKKINO_API_KEY")
        val poiskKinoApiBaseUrl = configValue("poiskkino.api.baseUrl", "POISKKINO_API_BASE_URL", "https://api.poiskkino.dev/")
        val geminiApiKey = configValue("gemini.api.key", "GEMINI_API_KEY")
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"${tmdbToken.escapeBuildConfigString()}\"")
        buildConfigField("String", "TMDB_API_BASE_URL", "\"${tmdbApiBaseUrl.escapeBuildConfigString()}\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"${tmdbImageBaseUrl.escapeBuildConfigString()}\"")
        buildConfigField("String", "POISKKINO_API_KEY", "\"${poiskKinoApiKey.escapeBuildConfigString()}\"")
        buildConfigField("String", "POISKKINO_API_BASE_URL", "\"${poiskKinoApiBaseUrl.escapeBuildConfigString()}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey.escapeBuildConfigString()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
