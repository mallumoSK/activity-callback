plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply("../secure-android.gradle")

group = "tk.mallumo"
version = "1.0.1"

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"
    buildFeatures.compose = true

    defaultConfig {
        minSdk = 21
        targetSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["compose_version"] as String
        @Suppress("ImplicitThis")
        kotlinCompilerVersion = rootProject.extra["kotlin_version"] as String
        useLiveLiterals = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xallow-jvm-ir-dependencies",
            "-XXLanguage:+NonParenthesizedAnnotationsOnFunctionalTypes")
    }
}

dependencies {

    implementation(project(":activity-callback"))

    implementation("androidx.appcompat:appcompat:${rootProject.extra["appcompat"]}")
    implementation("androidx.compose.ui:ui:${rootProject.extra["compose_version"]}")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}