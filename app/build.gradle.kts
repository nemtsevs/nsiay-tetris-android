import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    jacoco
}

val apkBaseName = "nsiay-tetris"

android {
    namespace = "nsi.example.tetris"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "nsi.example.tetris"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val variantName = variant.name
            (output as VariantOutputImpl).outputFileName.set("$apkBaseName-$variantName.apk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoDebugCoverage") {
    dependsOn("testDebugUnitTest")

    val buildDir = layout.buildDirectory.get().asFile

    reports {
        xml.required.set(true)   // Для CI / Codecov
        html.required.set(true)  // Для локального просмотра
        csv.required.set(false)
    }

    val excludes = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/android/**",
        "**/debugAndroidTest/**",
        "**/debugUnitTest/**"
    )

    classDirectories.setFrom(
        fileTree(buildDir) {
            include("intermediates/**/classes/**/*.class") // Java + новый Kotlin (AGP 9+)
            include("tmp/kotlin-classes/**/*.class")       // Kotlin (AGP 8-)
            exclude(excludes)
        }
    )

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(
        fileTree(buildDir) {
            include("**/unit_test_code_coverage/**/*.exec")
        }
    )
}