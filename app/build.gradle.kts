plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.duck.photopicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.duck.photopicker"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }
	
    signingConfigs {
        release {
            storeFile file(project.property('RELEASE_STORE_FILE'))
            storePassword project.property('RELEASE_STORE_PASSWORD')
            keyAlias project.property('RELEASE_KEY_ALIAS')
            keyPassword project.property('RELEASE_KEY_PASSWORD')
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
			isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    // If you want to use Material Design for the launcher layout (optional)
    // implementation("com.google.android.material:material:1.12.0")
}
