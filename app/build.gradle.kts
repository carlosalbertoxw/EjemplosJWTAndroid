plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Genera en compilación los adaptadores JSON de Moshi (sin reflexión).
    alias(libs.plugins.ksp)
}

// URL base de la API. Se puede sobreescribir sin tocar el código:
//   - en gradle.properties:  apiBaseUrl=http://192.168.1.75:8000/
//   - o por línea de comandos:  ./gradlew assembleDebug -PapiBaseUrl=...
// Por defecto, 10.0.2.2 = localhost de tu PC visto desde el emulador.
// Cualquiera de las APIs de ejemplo sirve (mismo contrato): PHP en el
// puerto 8000, Java en el 8080 y .NET en el 5274.
val apiBaseUrl: Provider<String> = providers.gradleProperty("apiBaseUrl")
    .orElse("http://10.0.2.2:8000/")

android {
    namespace = "com.carlosalbertoxw.ejemplos_jwt_android"
    // Compose BOM 2026.11 exige compilar contra la API 37.1 o superior.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.carlosalbertoxw.ejemplos_jwt_android"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "3.0"

        buildConfigField("String", "BASE_URL", "\"${apiBaseUrl.get()}\"")
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
        // Los avisos de "hay una versión más nueva" no dependen del código sino
        // del paso del tiempo: con warningsAsErrors romperían la CI cada vez que
        // se publica una versión, y cada PR de Dependabot (que sube una sola
        // dependencia) fallaría por las demás. Las versiones las propone Dependabot.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
    testOptions {
        // Robolectric necesita los recursos de Android para las pruebas de Compose.
        unitTests.isIncludeAndroidResources = true
    }
}

// El APK de release no permite HTTP en claro (la excepción para 10.0.2.2 solo
// existe en src/debug): sin una URL https:// compilaría bien pero fallaría en
// todas las peticiones. Se comprueba al construir el release, no en debug.
val checkReleaseApiBaseUrl = tasks.register("checkReleaseApiBaseUrl") {
    val url = apiBaseUrl
    doLast {
        check(url.get().startsWith("https://")) {
            "El release necesita una URL HTTPS: ./gradlew assembleRelease -PapiBaseUrl=https://... " +
                "(valor actual: ${url.get()})"
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(checkReleaseApiBaseUrl)
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
    // Pruebas de pantallas Compose en la JVM (sin emulador) con Robolectric
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
