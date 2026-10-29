import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "tech.eiki.globeswap"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.eiki.globeswap"
        minSdk = 29
        targetSdk = 36
        // versionCode zählt GitHub Actions automatisch hoch (GLOBESWAP_VERSION_CODE = 100 + Build-Nummer,
        // siehe .github/workflows/build.yml). Lokale Builds bekommen 4.
        versionCode = providers.environmentVariable("GLOBESWAP_VERSION_CODE").orNull?.toIntOrNull() ?: 4
        // Sichtbare Version für Nutzer. Bei einem Release-Tag (v1.4) setzt GitHub Actions sie aus dem Tag
        // (GLOBESWAP_VERSION_NAME), sonst gilt der Wert hier.
        versionName = providers.environmentVariable("GLOBESWAP_VERSION_NAME").orNull?.takeIf { it.isNotBlank() } ?: "1.3"

        val serverUrl = providers.gradleProperty("globeswap.serverUrl").get().trimEnd('/')
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")

        // Push (Firebase Cloud Messaging): google-services.json aus der Firebase-Konsole – lokal als
        // app/google-services.json, in GitHub Actions als Secret GOOGLE_SERVICES_JSON. Ohne die Datei
        // baut die App ohne Push und fragt wie bisher alle 15 Minuten nach.
        val firebase = firebaseConfig()
        listOf("PROJECT_ID", "SENDER_ID", "APP_ID", "API_KEY").forEach {
            buildConfigField("String", "FIREBASE_$it", "\"${firebase[it].orEmpty()}\"")
        }
    }

    // Signaturschlüssel liegt nicht im Projekt-Code: signing/keystore.properties (siehe README).
    val keystoreFile = rootProject.file("signing/keystore.properties")
    if (keystoreFile.exists()) {
        val keystore = Properties().apply { keystoreFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    // Zwei Varianten aus demselben Code: "github" (APK für GitHub/IzzyOnDroid/itch.io, kann sich selbst
    // aktualisieren) und "play" (App-Bundle für Google Play, ohne Installations-Berechtigung – Updates kommen
    // dort vom Play Store). Sie unterscheiden sich nur in src/github/AndroidManifest.xml und SELF_UPDATE.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "SELF_UPDATE", "false")
        }
    }

    buildTypes {
        release {
            // R8 entfernt ungenutzten Code und Ressourcen (v. a. ungenutzte Icons) – die APK wird deutlich kleiner.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperty("roborazzi.test.record", "true") }
        }
    }
    // Kein verschlüsselter Abhängigkeits-Block in der APK: F-Droid und IzzyOnDroid lehnen ihn ab.
    // Im App-Bundle für Google Play bleibt er (Google nutzt ihn für Hinweise zu Bibliotheken).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Glas-Effekt (Unschärfe hinter der Leiste unten), Apache 2.0; 1.2.x passt zu Compose 1.7
    implementation("dev.chrisbanes.haze:haze:1.2.2")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    val cameraX = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // UI-Tests ohne Gerät (Robolectric rendert echte Pixel, Roborazzi speichert Screenshots)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    testImplementation("io.coil-kt:coil-test:2.7.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/** Liest die Werte für Firebase aus google-services.json (ohne das Google-Services-Gradle-Plugin). */
fun firebaseConfig(): Map<String, String> {
    val text = providers.environmentVariable("GOOGLE_SERVICES_JSON").orNull?.takeIf { it.isNotBlank() }
        ?: file("google-services.json").takeIf { it.exists() }?.readText()
        ?: return emptyMap()
    val json = groovy.json.JsonSlurper().parseText(text) as Map<*, *>
    val project = json["project_info"] as Map<*, *>
    val client = (json["client"] as List<*>).map { it as Map<*, *> }.firstOrNull {
        val info = (it["client_info"] as Map<*, *>)["android_client_info"] as Map<*, *>
        info["package_name"] == "tech.eiki.globeswap"
    } ?: error("google-services.json enthält keine App mit dem Paketnamen tech.eiki.globeswap")
    return mapOf(
        "PROJECT_ID" to project["project_id"].toString(),
        "SENDER_ID" to project["project_number"].toString(),
        "APP_ID" to (client["client_info"] as Map<*, *>)["mobilesdk_app_id"].toString(),
        "API_KEY" to ((client["api_key"] as List<*>).first() as Map<*, *>)["current_key"].toString(),
    )
}
