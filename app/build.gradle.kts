plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "io.github.qqclutchyqq.crossroad.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.qqclutchyqq.crossroad.android"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-poc"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
    sourceSets["main"].assets.srcDir("build/generated/runtime-assets")
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Fixed CI debug identity survives builds; never use it as a release key.
    signingConfigs.getByName("debug") {
        storeFile = rootProject.file(".debug/debug.keystore")
    }
}
dependencies {
    implementation("androidx.webkit:webkit:1.14.0")
    testImplementation("junit:junit:4.13.2")
}
tasks.register("verifyRuntimeAssets") {
    doLast {
        check(file("build/generated/runtime-assets/web/runtime-worker.js").isFile) {
            "Run node scripts/prepare-runtime.mjs before building."
        }
    }
}
tasks.named("preBuild") { dependsOn("verifyRuntimeAssets") }
