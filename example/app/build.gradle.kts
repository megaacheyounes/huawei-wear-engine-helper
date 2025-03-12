plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.huawei.agconnect")
}

android {

    namespace = "com.younes.wearenginehelper"
    compileSdk = 34

    val vMajor = 1
    val vMinor = 0
    val vPatch = 0

    val versionC = ((vMajor * 100) + vMinor) * 1000 + vPatch

    compileSdk = 34

    defaultConfig {
        //TODO: update package name
        applicationId = "com.younes.wearenginehelper"
        minSdk = 26
        targetSdk = 34

        versionCode = versionC
        versionName = "${vMajor}.${vMinor}.${vPatch}"

        println("versionName: $versionName, versionCode:$versionC")

        val versionName = android.defaultConfig.versionName
        setProperty("archivesBaseName", "WearEngineHelperSample-$versionName-$versionCode")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }


    signingConfigs {
        create("main") {
            //TODO: add your keystore
            storeFile = file("keystore.jks")
            storePassword = "1234"
            keyAlias = "main"
            keyPassword = "1234"
        }
    }

    buildTypes {

        debug {
            versionNameSuffix = "-DEBUG"
            signingConfig = signingConfigs.getByName("main")
        }
        release {
            signingConfig = signingConfigs.getByName("main")
            isMinifyEnabled = true //true
            isDebuggable = false

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
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        //todo: reset https://developer.android.com/reference/tools/gradle-api/7.4/com/android/build/api/dsl/Lint
        checkReleaseBuilds = false
        abortOnError = false
        ignoreWarnings = true
        ignoreTestSources = true
        checkDependencies = false
    }


}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Compose dependencies
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.paging:paging-compose:3.2.0")
    implementation("androidx.activity:activity-compose:1.7.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)


    //hms
    implementation("com.huawei.agconnect:agconnect-core:1.9.1.301")

    //wearengine
    implementation("com.huawei.hms:wearengine:5.0.3.303")

    //test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}