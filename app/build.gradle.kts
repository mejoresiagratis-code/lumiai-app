plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.mejoresiagratis.lumiai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mejoresiagratis.lumiai"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "0.7.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("lumiai-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // IDs de PRUEBA de Google en debug: nunca generan impresiones reales.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            isMinifyEnabled = true
            // IDs reales de la cuenta de AdMob de LumiAI.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4452549520942931~7390634923"
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"ca-app-pub-4452549520942931/3592393086\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// jvmTarget en el DSL moderno de Kotlin. Sustituye al bloque `kotlinOptions` de `android {}`,
// deprecado en AGP 8.x y ELIMINADO en AGP 9: migrarlo ahora evita repetir el trabajo.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Los tests Robolectric (semántica de Compose) descargan el JAR de Android y arrancan un
// entorno emulado en JVM: son, con diferencia, lo más lento de `testDebugUnitTest`. En CI
// los excluimos del camino crítico con `-PexcludeSlowTests` para acelerar cada push,
// manteniendo intactos los tests de dominio (baratos). En local, sin la flag, corre TODO.
// Se marcan con el JUnit @Category(SlowTest::class); ver com.mejoresiagratis.lumiai.testing.
tasks.withType<Test>().configureEach {
    if (project.hasProperty("excludeSlowTests")) {
        useJUnit {
            excludeCategories("com.mejoresiagratis.lumiai.testing.SlowTest")
        }
    }
}

// AdMob 25.x se compila con Kotlin 2.2 (metadata 2.2.0). Con Kotlin 2.2.10 ya no debería hacer
// falta, pero se mantiene como red: es inofensivo y solo afecta a la lectura de binarios de
// terceros (no a nuestro código). Candidato a retirar cuando el build esté verde y estable.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.kolor)
    implementation(libs.haze)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.billing.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Alerta sonora (deteccion de sonidos en el dispositivo). Requiere yamnet.tflite en assets.
    implementation(libs.mediapipe.tasks.audio)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
