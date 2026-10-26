plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.flyfly6.aerialclean"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.flyfly6.aerialclean"
        minSdk = 19
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        // 唯一硬闸门：NewApi。其余规则对 4.4 目标没有否决权，避免噪声压住真正的兼容问题。
        checkOnly.add("NewApi")
        abortOnError = true
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation("junit:junit:4.13.2")
}
