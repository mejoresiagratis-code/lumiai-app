// `java` dentro del script es el ACCESSOR de JavaPluginExtension, no el paquete:
// java.util.Base64 inline no compila (memo #32). Import de cabecera y listo.
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.mejoresiagratis.lumiai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mejoresiagratis.lumiai"
        minSdk = 24
        targetSdk = 36
        versionCode = 69
        versionName = "0.9.40"
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
        // Firma de RELEASE desde secretos de GitHub Actions (nunca commiteada).
        // Sin secretos (PRs, builds locales) cae a la de debug: el APK minificado se puede
        // INSTALAR para smoke en dispositivo, pero Play jamas aceptara esa firma — es a
        // proposito: imposible publicar por accidente una build sin la clave real.
        create("release") {
            val ksB64 = System.getenv("LUMI_KEYSTORE_BASE64")
            if (!ksB64.isNullOrBlank()) {
                val ksFile = layout.buildDirectory.file("lumi-release.keystore").get().asFile
                ksFile.parentFile.mkdirs()
                // Los visores de texto parten lineas largas metiendo espacios, saltos e incluso
                // GUIONES de silabeo (confirmado por Pablo, memo #33). El alfabeto base64 es
                // cerrado (A-Za-z0-9+/=), asi que todo lo ajeno a el se descarta sin riesgo:
                // el secreto decodifica aunque venga de un copy-paste sucio.
                val clean = ksB64.filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
                ksFile.writeBytes(Base64.getDecoder().decode(clean))
                storeFile = ksFile
                storePassword = System.getenv("LUMI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LUMI_KEY_ALIAS")
                keyPassword = System.getenv("LUMI_KEY_PASSWORD")
            } else {
                storeFile = file("lumiai-debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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
            signingConfig = signingConfigs.getByName("release")
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

    lint {
        // Gate de calidad: solo los errores reales bloquean release (Q4).
        // Warnings se reportan pero no bloquean; actualizar versiones de dependencias
        // es trabajo de mantenimiento programado, no urgente.
        warningsAsErrors = false
        abortOnError = true
        // Categorias silenciadas con justificacion:
        // GradleDependency/NewerVersionAvailable: actualizaciones programadas en cada
        //   toolchain bump, no en cada PR (leccion #18: separar compilador de libs).
        // UnusedResources: falsos positivos frecuentes con Compose (strings en code-only,
        //   drawables usados via stringResource/painterResource sin referencia XML).
        disable += setOf("GradleDependency", "NewerVersionAvailable", "UnusedResources",
            "AndroidGradlePluginVersion")
        // El informe XML lo consume el step de CI para subir el artefacto.
        xmlReport = true
        htmlReport = true
        xmlOutput = file("build/reports/lint-results-debug.xml")
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
    implementation(libs.firebase.crashlytics)
    // Solo debug: detector de fugas de memoria. Con servicios foreground de linterna y
    // microfono, una Activity retenida es plausible y barata de cazar ahora.
    // App Check (Q5): Play Integrity va en AMBAS variantes; el proveedor de depuracion
    // SOLO en debug — que la clase de debug ni exista en el APK de release es la parte
    // que hace esto robusto de verdad, no solo un if de BuildConfig.
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    debugImplementation(libs.leakcanary)
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
