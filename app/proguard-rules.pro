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
