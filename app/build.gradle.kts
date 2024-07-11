plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ca_test"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ca_test"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.glide)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.play.services.location)
    implementation(libs.core.ktx) // 添加 AndroidX core-ktx 库
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
