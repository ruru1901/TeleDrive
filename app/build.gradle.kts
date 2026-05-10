import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.teledrive.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teledrive.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }
    fun signingValue(envName: String): String? =
        System.getenv(envName) ?: keystoreProperties.getProperty(envName)

    val releaseKeystoreFile = signingValue("KEYSTORE_FILE")?.let { rootProject.file(it) }
    val releaseKeyAlias = signingValue("KEY_ALIAS")
    val releaseKeyPassword = signingValue("KEY_PASSWORD")
    val releaseStorePassword = signingValue("STORE_PASSWORD")
    val releaseSigningReady = releaseKeystoreFile?.isFile == true &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = releaseKeystoreFile
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storePassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseSigningReady) {
                signingConfig = signingConfigs.findByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(layout.buildDirectory.dir("generated/jnaJniLibs"))
        }
    }
}

val jnaNative by configurations.creating

val extractJnaNativeLibs by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/jnaJniLibs")
    outputs.dir(outputDir)

    doLast {
        val jnaJar = jnaNative.singleFile
        val mappings = mapOf(
            "com/sun/jna/linux-aarch64/libjnidispatch.so" to "arm64-v8a/libjnidispatch.so",
            "com/sun/jna/linux-arm/libjnidispatch.so" to "armeabi-v7a/libjnidispatch.so",
            "com/sun/jna/linux-x86/libjnidispatch.so" to "x86/libjnidispatch.so",
            "com/sun/jna/linux-x86-64/libjnidispatch.so" to "x86_64/libjnidispatch.so",
        )
        mappings.forEach { (sourcePath, targetPath) ->
            copy {
                from(zipTree(jnaJar)) {
                    include(sourcePath)
                    eachFile { path = targetPath }
                    includeEmptyDirs = false
                }
                into(outputDir)
            }
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn(extractJnaNativeLibs)
}

dependencies {
    implementation(project(":feature:auth"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:drive"))

    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.sqlite)
    implementation(libs.tdlibx)
    implementation(libs.lazyscrollbar)
    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.lazysodium)
    implementation(libs.lazysodium.jna)
    jnaNative(libs.lazysodium.jna)
    implementation(libs.sqlcipher)
    implementation(libs.argon2kt)
    implementation(libs.kserializer)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
