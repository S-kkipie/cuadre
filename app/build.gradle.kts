plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    // Reads app/google-services.json (Firebase project cuadre-pe). The file is git-ignored.
    id("com.google.gms.google-services")
}

android {
    namespace = "pe.aido.cuadre"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.aido.cuadre"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // cuadre-backend base URL (e.g. https://cuadre-backend.vercel.app). Empty = sharing off.
        val apiUrl = (project.findProperty("cuadreApiUrl") as String?).orEmpty()
        buildConfigField("String", "CUADRE_API_URL", "\"$apiUrl\"")
        // Google sign-in: the Web OAuth client ID (project cuadre-pe). Empty = button hidden.
        val googleWebClientId = (project.findProperty("googleWebClientId") as String?).orEmpty()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Uploads to the store survive no-signal moments and app restarts.
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Payments from the store's other phones arrive as FCM data messages (only once linked).
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
    // Firebase drags in an old androidx.fragment; the permission request (ActivityResult API) needs >= 1.3.
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // "Continuar con Google" via Credential Manager.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // real org.json on the JVM (Android's is a stub)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
