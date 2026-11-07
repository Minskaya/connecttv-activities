plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.minskaya.connecttv"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.minskaya.connecttv.activities"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    flavorDimensions += "tile"
    productFlavors {
        create("guide") { dimension = "tile"; applicationIdSuffix = ".guide" }
        create("tv") { dimension = "tile"; applicationIdSuffix = ".tv" }
        create("replay") { dimension = "tile"; applicationIdSuffix = ".replay" }
        // Pas une tuile : service d'accessibilité qui renvoie la touche Home vers Projectivy
        create("homekey") { dimension = "tile"; applicationIdSuffix = ".homekey" }
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
