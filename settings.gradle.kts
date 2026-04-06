import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project

// Compilar fuera de OneDrive/Proyecto para evitar bloqueos de archivos (Unable to delete directory)
val customBuildRoot = File(System.getProperty("user.home"), "bloqueo_build").apply { mkdirs() }
gradle.beforeProject(Action<Project> {
    val dir = File(customBuildRoot, path.replace(":", "_").replace("_", File.separator).trimStart(File.separatorChar).ifEmpty { "root" })
    layout.buildDirectory.set(dir)
})

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Bloqueo"
include(":app")
