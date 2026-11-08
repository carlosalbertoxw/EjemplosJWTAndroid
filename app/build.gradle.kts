plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Genera en compilación los adaptadores JSON de Moshi (sin reflexión).
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.carlosalbertoxw.ejemplos_jwt_android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.carlosalbertoxw.ejemplos_jwt_android"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "3.0"

        // URL base de la API. Se puede sobreescribir sin tocar el código:
        //   - en gradle.properties:  apiBaseUrl=http://192.168.1.75:8000/
        //   - o por línea de comandos:  ./gradlew assembleDebug -PapiBaseUrl=...
        // Por defecto, 10.0.2.2 = localhost de tu PC visto desde el emulador.
        // Cualquiera de las APIs de ejemplo sirve (mismo contrato): PHP en el
        // puerto 8000, Java en el 8080 y .NET en el 5274.
        val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
            .getOrElse("http://10.0.2.2:8000/")
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            // R8 reduce y ofusca el APK de release (Retrofit, OkHttp y los
            // adaptadores generados de Moshi traen sus propias reglas).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Análisis estático: en CI el build falla ante cualquier aviso de Android Lint.
    lint {
        warningsAsErrors = true
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Red: cliente HTTP + serialización JSON (Moshi valida la null-safety
    // de Kotlin al deserializar, a diferencia de Gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp.logging.interceptor)

    // Persistencia de los tokens de sesión
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    // Servidor HTTP falso para probar el flujo 401 -> refresh -> reintento
    testImplementation(libs.okhttp.mockwebserver)
}
