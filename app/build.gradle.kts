plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "it.lmqv.livematchcam"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.lmqv.livematchcam"
        minSdk = 26
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 27
        versionCode = 1
        versionName = "2.5"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    packagingOptions {
        jniLibs {
            pickFirsts += setOf(
                "lib/x86/libUVCCamera.so",
                "lib/x86/libuvc.so",
                "lib/x86_64/libUVCCamera.so",
                "lib/x86_64/libuvc.so",
                "lib/armeabi-v7a/libUVCCamera.so",
                "lib/armeabi-v7a/libuvc.so",
                "lib/arm64-v8a/libUVCCamera.so",
                "lib/arm64-v8a/libuvc.so"
            )
        }
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.ktx)
    implementation(libs.rootencoder.library)
    implementation(libs.rootencoder.extra.sources)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.com.google.code.gson)
    implementation(libs.org.jetbrains.kotlin.reflect)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.youtube)
    implementation(libs.play.services.base)
    implementation(libs.play.services.auth)
    implementation(libs.com.squareup.okhttp3)
    implementation(libs.google.http.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.coil.kt)
    implementation(libs.com.google.zxing.core)
    implementation(libs.com.google.android.gms)
    implementation(libs.com.google.firebase.bom)
    implementation(libs.com.google.firebase.database.ktx)
    implementation(libs.com.jiangdongguo.libausbc)
    //implementation(libs.com.jiangdongguo.libuvc)
    //implementation(libs.com.jiangdongguo.libuvccommon)
    //implementation(libs.com.jiangdongguo.libnative)
    implementation(libs.com.herohan.uvcandroid)

    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.compose)
    //implementation(libs.compose.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}