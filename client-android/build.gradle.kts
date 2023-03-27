plugins {
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.nordic.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

group = "no.nordicsemi.android.kotlin.ble.client.real"

android {
    namespace = "no.nordicsemi.android.kotlin.ble.client.real"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":client-api"))

    implementation(libs.nordic.core)
}
