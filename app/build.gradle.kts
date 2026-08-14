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
        versionCode = 4
        versionName = "0.66"
    }
	
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("RELEASE_STORE_FILE") as String)
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String
			storeType = "JKS"   
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
