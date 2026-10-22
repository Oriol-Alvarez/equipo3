import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Notificaciones push: Firebase se activa solo si existe app/google-services.json
// (se descarga de la Consola de Firebase y no se sube a Git). Sin él la app funciona igual.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Firma del APK de release: los datos viven en keystore.properties (no se sube a git).
// Ver docs/entrega-final.md para generar la llave.
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// Servidor con el que se compila la app. Se puede cambiar sin tocar el código:
//   gradlew assembleRelease -PapiUrl=https://mi-tunel.trycloudflare.com/api/v1/
// y también desde la app (pantalla de inicio de sesión > "Servidor").
val apiUrl = (project.findProperty("apiUrl") as String?)
    ?: "https://point-shareholders-mart-con.trycloudflare.com/api/v1/"

android {
    namespace = "com.example.etapa1"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.etapa1"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.0"

        buildConfigField("String", "API_URL", "\"$apiUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (firma.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // RF-20: el APK que se entrega siempre bloquea las capturas de pantalla
            buildConfigField("boolean", "PERMITIR_CAPTURAS", "false")
        }
        debug {
            // Solo para tomar las capturas del manual de usuario o depurar:
            //   gradlew assembleDebug -PpermitirCapturas=true
            val permitir = (project.findProperty("permitirCapturas") as String?) == "true"
            buildConfigField("boolean", "PERMITIR_CAPTURAS", permitir.toString())
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
    testOptions {
        // android.util.Log y similares devuelven valores vacíos en las pruebas de JVM
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Capa de Red y API REST (Retrofit, OkHttp, Kotlinx Serialization)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase Cloud Messaging (Push Notifications)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Revisión periódica de anuncios y mensajes cuando la app está cerrada (respaldo sin Firebase)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // AndroidX Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // RF-13: desbloqueo con huella (BiometricPrompt)
    implementation("androidx.biometric:biometric:1.1.0")


    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}