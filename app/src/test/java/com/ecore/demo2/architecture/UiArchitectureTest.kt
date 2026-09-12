package com.ecore.demo2.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Protege la separación UI / lógica mientras el equipo rediseña pantallas.
 *
 * Los archivos de diseño (feature/.../XxxScreen.kt y feature/.../components/) solo deben usar
 * UiState, modelos de dominio y componentes de UI. Nunca repositorios, red, base de datos,
 * DataStore, DI ni la navegación global.
 */
class UiArchitectureTest {

    // Los tests unitarios de Android se ejecutan con el módulo `app/` como directorio de trabajo.
    private val featureDir = File("src/main/java/com/ecore/demo2/feature")

    private val forbiddenPackages = listOf(
        "com.ecore.demo2.core.repository",
        "com.ecore.demo2.core.network",
        "com.ecore.demo2.core.database",
        "com.ecore.demo2.core.datastore",
        "com.ecore.demo2.core.di",
        "com.ecore.demo2.navigation",
        "retrofit2",
        "okhttp3",
        "androidx.room",
        "androidx.datastore",
    )

    private fun uiFiles(): List<File> = featureDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filter { it.name.endsWith("Screen.kt") || it.parentFile?.name == "components" }
        .toList()

    @Test
    fun `ui files are found`() {
        assertTrue("No se encuentra ${featureDir.absolutePath}", featureDir.isDirectory)
        assertTrue("Se esperaban las pantallas y componentes de las features", uiFiles().size >= 13)
    }

    @Test
    fun `screens and components do not import data layer, di or global navigation`() {
        val violations = uiFiles().flatMap { file ->
            file.readLines()
                .filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").trim() }
                .filter { import -> forbiddenPackages.any { import == it || import.startsWith("$it.") } }
                .map { "${file.path}: $it" }
        }

        assertTrue(
            "Las pantallas solo pueden usar UiState, modelos y componentes de UI. Imports no permitidos:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
