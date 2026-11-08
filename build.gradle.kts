// Top-level build file where you can add configuration options common to all sub-projects/modules.
// AGP 9 trae soporte de Kotlin integrado: ya no se aplica el plugin kotlin-android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
