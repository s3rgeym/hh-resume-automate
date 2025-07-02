plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.s3rgeym.hh_resume_automate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.s3rgeym.hh_resume_automate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Эти настройки нужны только через сборки через Github Workflow.
        // Локально же они ммешают.
        if (
            project.hasProperty("KEYSTORE_PATH") &&
            project.hasProperty("KEYSTORE_PASSWORD") &&
            project.hasProperty("KEY_ALIAS") &&
            project.hasProperty("KEY_PASSWORD")
        ) {
            create("release") {
                storeFile = file("../" + project.property("KEYSTORE_PATH") as String)
                storePassword = project.property("KEYSTORE_PASSWORD") as String
                keyAlias = project.property("KEY_ALIAS") as String
                keyPassword = project.property("KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false // слишком долго компилируется, а ресурс тактов на компиляцию ограничен
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isMinifyEnabled = false
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13" // или последняя стабильная
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Foundation (нужно для LazyColumn)
    implementation("androidx.compose.foundation:foundation")

    // Material 3
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Compose runtime + lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // HTTP-клиент
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Воркеры
    implementation(libs.androidx.work.runtime.ktx)

    // Тесты
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
