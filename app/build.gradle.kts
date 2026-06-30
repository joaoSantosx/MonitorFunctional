import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jvf.monitorfunctional"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
     kotlinCompilerExtensionVersion = "1.5.1"
    }

    defaultConfig {
        applicationId = "com.jvf.monitorfunctional"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        buildConfigField("String", "youtube_api_key", properties.getProperty("youtube_api_key") ?: "\"\"")
        buildConfigField("String", "gemini_api_key", properties.getProperty("gemini_api_key") ?: "\"\"")
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation("com.google.firebase:firebase-functions")
    implementation (composeBom)
    implementation ("androidx.compose.material:material-icons-extended:1.7.8")
    implementation ("com.google.firebase:firebase-messaging")
    implementation("androidx.compose.ui:ui")
    implementation ("androidx.compose.material3:material3")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("androidx.activity:activity-compose:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.credentials:credentials:1.3.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(libs.room.runtime)
    implementation("androidx.room:room-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation(libs.firebase.crashlytics)
    ksp(libs.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}