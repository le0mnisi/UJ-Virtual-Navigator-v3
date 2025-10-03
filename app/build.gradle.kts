plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ujvirtualnavigatorv3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ujvirtualnavigatorv3"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.mapbox.maps:android-ndk27:11.15.0")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.15.0")

// Mapbox Navigation SDK (NDK 27)
    implementation("com.mapbox.navigationcore:android-ndk27:3.12.0")
    implementation("com.mapbox.navigationcore:ui-maps-ndk27:3.12.0")
    implementation("com.mapbox.navigationcore:ui-components-ndk27:3.12.0")
    implementation("com.mapbox.navigationcore:voice-ndk27:3.12.0")
    implementation("com.mapbox.navigationcore:tripdata-ndk27:3.12.0")
    implementation("com.mapbox.navigationcore:copilot-ndk27:3.12.0")

// Mapbox Search SDK (NDK 27)
    implementation("com.mapbox.search:autofill-ndk27:2.14.0")
    implementation("com.mapbox.search:discover-ndk27:2.14.0")
    implementation("com.mapbox.search:place-autocomplete-ndk27:2.14.0")
    implementation("com.mapbox.search:offline-ndk27:2.14.0")
    implementation("com.mapbox.search:mapbox-search-android-ndk27:2.14.0")
    implementation("com.mapbox.search:mapbox-search-android-ui-ndk27:2.14.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}