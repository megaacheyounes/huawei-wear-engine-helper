buildscript {
    dependencies {
        classpath ("com.android.tools.build:gradle:8.2.0")
        classpath ("com.huawei.agconnect:agcp:1.9.1.301")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    alias(libs.plugins.hilt) apply false
}