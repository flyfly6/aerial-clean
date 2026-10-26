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

    buildTypes {
        release {
            // C5：APK < 1.5MB。体积几乎全是 androidx.core 传递来的 kotlin-stdlib，
            // 而我们只用到 RecyclerView，R8 能把没引用的全部剪掉。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 本机验收用 debug 签名，省掉生成正式密钥的步骤。
            signingConfig = signingConfigs.getByName("debug")
        }
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
