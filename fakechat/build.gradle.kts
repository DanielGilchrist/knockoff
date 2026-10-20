plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.danielgilchrist.knockoff.fakechat"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.danielgilchrist.knockoff.fakechat"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}
