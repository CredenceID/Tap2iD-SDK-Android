import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

val nexusPropertiesFile = file("nexus.properties")
val nexusProperties = Properties()
nexusProperties.load(nexusPropertiesFile.inputStream())
val nexusUrl: String = nexusProperties.getProperty("nexus_url")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")

        //--------------Nexus Configuration-------------------
        maven {
            url = uri(nexusUrl)
        }
        //--------------Nexus Configuration-------------------
    }
}

rootProject.name = "Tap2iDSdkSample"
include(":app")
