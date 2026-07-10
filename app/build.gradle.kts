plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.carlosalbertoxw.ejemplos_jwt_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carlosalbertoxw.ejemplos_jwt_android"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        // URL base de la API. Se puede sobreescribir sin tocar el código:
        //   - en gradle.properties:  apiBaseUrl=http://10.0.2.2:8000/
        //   - o por línea de comandos:  ./gradlew assembleDebug -PapiBaseUrl=...
        // Recuerda: 10.0.2.2 = localhost de tu PC visto desde el emulador;
        // en dispositivo físico usa la IP de tu PC en la red local.
        val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
            .getOrElse("http://192.168.1.75:8000/")
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Red: cliente HTTP + serialización JSON (Moshi valida la null-safety
    // de Kotlin al deserializar, a diferencia de Gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging.interceptor)

    // Persistencia de los tokens de sesión
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    // Servidor HTTP falso para probar el flujo 401 -> refresh -> reintento
    testImplementation(libs.okhttp.mockwebserver)
}
