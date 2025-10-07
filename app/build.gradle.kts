plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.googleGms)
}

android {
    namespace = "com.example.tayabastrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tayabastrack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activityKtx)
    implementation(libs.constraintlayout)

    // Firebase BOM
    implementation(platform(libs.firebaseBom))

    // Firebase libraries
    implementation(libs.firebaseAuthKtx)
    implementation(libs.firebaseFirestoreKtx)
    implementation(libs.firebaseStorageKtx)

    // Google Maps API - Kotlin DSL syntax
    implementation(libs.playServicesMaps)
    implementation(libs.playServicesLocation)

    // Optional: Google Places API (for location search, autocomplete, etc.)
    implementation(libs.places)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.espressoCore)
}