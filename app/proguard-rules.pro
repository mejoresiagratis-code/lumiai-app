# LumiAI · reglas R8 de release (Q1, 12-ago-2026)
#
# Auditado el 12-ago: el codigo de la app NO usa reflexion propia (cero Class.forName /
# getMethod; el antiguo FirebaseManager reflexivo ya no existe). Hilt, Compose, Billing,
# Firebase y DataStore traen sus consumer rules en el artefacto. Por eso este fichero es
# corto A PROPOSITO: cada -keep innecesario es codigo muerto sin optimizar.

# Crashlytics: stacktraces con fichero y linea legibles tras la ofuscacion.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Los logs de debug no viajan a produccion.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Clases de COMPILE-TIME arrastradas por el procesador AutoValue empaquetado dentro de una
# dependencia: jamas existen en Android en runtime y nadie las invoca. R8 exige declarar
# que su ausencia es esperada (memo #34; lista tomada del missing_rules.txt que genera R8).
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**

# MediaPipe Tasks Audio (Alerta Sonora, clasificador YAMNet) — EXCEPCION deliberada a la
# filosofia de fichero corto de arriba. MediaPipe lee sus mensajes protobuf (proto-lite,
# GeneratedMessageLite) por REFLEXION en runtime, y sus consumer-rules del AAR NO son
# suficientes con R8 (confirmado: multiples issues abiertos y sin resolver en el propio
# repo de Google — github.com/google-ai-edge/mediapipe/issues/6138, /5141, /3509 — el
# fallo tipico es "Field xxx_ for Yyy not found" nada mas llamar a createFromOptions()).
# Sin esto, MediaPipeSoundClassifier.start() lanza una excepcion NO capturada por su
# catch(RuntimeException) — probablemente la causa real del crash/fallo silencioso de
# Alerta Sonora en release (QA 13/14-ago).
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
# El runtime de protobuf-lite tambien entra en el baile de reflexion (mediapipe#6138
# muestra fallos con clases protobuf ofuscadas tipo "j3.d"): mantenerlo entero es barato
# (protobuf-lite es pequeno) y elimina la variable de una vez.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# QA 14-ago (captura de Pablo, v0.9.23 release): ExceptionInInitializerError en el
# clasificador CON las reglas de arriba ya aplicadas — este bug va mas hondo que los
# keeps de mediapipe/protobuf. La causa raiz documentada en mediapipe#6138 es flogger
# (el logger de Google que MediaPipe usa): su deteccion de "quien me llama" camina la
# pila de llamadas identificando sus propios frames POR NOMBRE DE CLASE; la ofuscacion
# y el inlining de R8 los rompen y el <clinit> de TaskRunner muere. Keeps dirigidos:
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Y el seguro de vida: -dontobfuscate GLOBAL — la UNICA solucion CONFIRMADA como
# funcional por los reportes del propio repo de MediaPipe (#5141, textual: "App works
# in release mode also if I have this proguard-rules.pro: -dontobfuscate -keep class
# com.google.mediapipe.** {*;}"). Coste real: los nombres de clases no se renombran
# (la app es algo mas legible al ingenieria-inversa); el SHRINKING y la OPTIMIZACION
# de R8 siguen activos (el tamano apenas cambia). Beneficio: fiabilidad garantizada
# HOY, con deadline de Play el 31-ago. Revisar en v1.1: quitar esta linea, probar en
# dispositivo si los keeps dirigidos bastan por si solos, y reactivar la ofuscacion.
-dontobfuscate

