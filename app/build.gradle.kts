import java.util.Properties

plugins {
    id("com.android.application")
}

// Release signing key. It is git-ignored on purpose (never publish a private key): create
// keystore/keystore.properties next to your .jks file, or run scripts/build-apk.sh once.
val keystoreProperties = rootProject.file("keystore/keystore.properties")

android {
    namespace = "io.github.yarom1.hebrewletters"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.yarom1.hebrewletters"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (keystoreProperties.exists()) {
            create("release") {
                val props = Properties().apply { keystoreProperties.inputStream().use { load(it) } }
                storeFile = rootProject.file("keystore/" + props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreProperties.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // Lets a debug build sit next to the real app on the same phone.
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
