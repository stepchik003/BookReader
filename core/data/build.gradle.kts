import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.data"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()){
                load(localPropertiesFile.inputStream())
            }
        }

        buildConfigField("String", "ACCESS_KEY", localProperties.getProperty("access.key"))
        buildConfigField("String", "SECRET_KEY", localProperties.getProperty("secret.key"))
        buildConfigField("String", "REGION", localProperties.getProperty("region"))
        buildConfigField("String", "ENDPOINT", localProperties.getProperty("endpoint"))
        buildConfigField("String", "BUCKET_NAME", localProperties.getProperty("bucket.name"))

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

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation("com.google.firebase:firebase-firestore")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":core:domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("software.amazon.awssdk:s3:2.39.1")
    implementation("software.amazon.awssdk:auth-crt:2.39.1")
    implementation("software.amazon.awssdk:s3-transfer-manager:2.39.1")
    implementation(platform("software.amazon.awssdk:bom:2.39.1"))
}