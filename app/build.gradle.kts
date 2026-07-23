@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.impl.VariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    //noinspection NewerVersionAvailable
    id("com.mikepenz.aboutlibraries.plugin") version "14.2.0"
    id("com.github.ben-manes.versions") version "0.54.0"
}

android {
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.daisukikaffuchino.han1meviewer"
        minSdk = 29
        targetSdk = 37
        versionCode = 260723
        versionName = "26.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("int", "SEARCH_YEAR_RANGE_END", "${Config.thisYear}")

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                abiFilters += "arm64-v8a"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    splits {
        abi {
            isEnable = gradle.startParameter.taskRequests.toString().contains("Release")
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_new"
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_debug"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    lint {
        disable += setOf("EnsureInitializerMetadata")
    }
    namespace = "io.github.daisukikaffuchino.han1meviewer"

    @Suppress("UnstableApiUsage")
    androidResources {
        generateLocaleConfig = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.value(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-jvm-default=enable"
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val apkName = "Han1meViewer-v${output.versionName.get()}.apk"
            (output as VariantOutputImpl).outputFileName = apkName
        }
    }
}

dependencies {
    implementation(libs.aboutlibraries.core)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.startup.runtime)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.bundles.android.base)
    implementation(libs.bundles.android.jetpack)

    implementation(platform(libs.compose.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui.ui.tooling.preview)
    implementation(libs.androidx.ui)
    debugImplementation(libs.compose.ui.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.compose.avatar.cropper)
    implementation(libs.kyant.m3color)
    implementation(libs.sonner)

    implementation(libs.datetime)
    implementation(libs.serialization.json)
    implementation(libs.jsoup)

    implementation(libs.retrofit)
    implementation(libs.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dns.over.https)

    implementation(libs.coil)

    implementation(libs.jiaozi.video.player)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.mpv.lib)

    implementation(libs.spannable.x)

    ksp(libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    androidTestImplementation(libs.test.junit)
}
