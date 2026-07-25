package com.mejoresiagratis.lumiai.testing

/**
 * Marca JUnit para tests costosos (p. ej. Robolectric: descargan el JAR de Android y
 * arrancan un entorno emulado en JVM). En CI se excluyen del camino crítico con la flag
 * de Gradle `-PexcludeSlowTests` (ver app/build.gradle.kts). En local corren siempre.
 *
 * Uso: `@org.junit.experimental.categories.Category(SlowTest::class)` sobre la clase.
 */
interface SlowTest
