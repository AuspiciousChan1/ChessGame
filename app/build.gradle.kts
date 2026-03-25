import org.gradle.api.tasks.Sync
import java.io.FileInputStream
import java.util.Properties

val stockfishAssetRoot = layout.projectDirectory.dir("src/main/assets/stockfish")
val filteredMainAssetsDir = layout.buildDirectory.dir("generated/mainAssets")
val generatedStockfishJniLibsDir = layout.buildDirectory.dir("generated/stockfishJniLibs")

val prepareMainAssets by tasks.registering(Sync::class) {
    from(layout.projectDirectory.dir("src/main/assets"))
    exclude("stockfish/**")
    into(filteredMainAssetsDir)
}

val prepareStockfishJniLibs by tasks.registering(Sync::class) {
    from(stockfishAssetRoot)
    include("**/libstockfish.so")
    exclude { !it.isDirectory && it.file.length() == 0L }
    into(generatedStockfishJniLibsDir)
    doLast {
        val sourceFiles = stockfishAssetRoot.asFileTree.matching {
            include("**/libstockfish.so")
        }.files
        val emptyFiles = sourceFiles.filter { it.length() == 0L }
        if (emptyFiles.isNotEmpty()) {
            logger.warn("Skipping empty Stockfish binaries: ${emptyFiles.joinToString { it.relativeTo(projectDir).path }}")
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.chenjili.chessgame"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.chenjili.chessgame"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // 开启混淆
            isShrinkResources = true // 开启资源压缩 (配合 minify 使用)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 配置签名
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets {
        getByName("main") {
            assets.setSrcDirs(listOf(filteredMainAssetsDir.get().asFile))
            jniLibs.srcDir(generatedStockfishJniLibsDir.get().asFile)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(project(":chess"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.named("preBuild") {
    dependsOn(prepareMainAssets, prepareStockfishJniLibs)
}
