plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alas.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alas.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // 指定目标游戏包名，供自动启动/停止游戏使用(默认为国服 Azur Lane)
        buildConfigField("String", "DEFAULT_GAME_PACKAGE", "\"com.bilibili.azurlane\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE")
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD")

            if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 如果环境变量中配置了签名信息，则使用 release 签名配置
            val hasSigningConfig = System.getenv("SIGNING_STORE_FILE") != null
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // OpenCV 自带若干架构的 .so；按需剔除以免包体膨胀
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // AndroidX / Compose UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // OpenCV 图像识别层(社区维护的 Android AAR 包，含 org.opencv.android.* 与各架构 .so)
    implementation("com.quickbirdstudios:opencv:4.9.0")

    // JSON 配置解析
    implementation("org.json:json:20240303")

    // 本地 ADB 协议实现(可选)：自研精简 ADB 客户端，用于"无线调试"自控连接本机
    // 注：正式引入时解包 com.android.tools 或使用 adblib 方案；此处在核心层提供接口。
    testImplementation("junit:junit:4.13.2")
}
