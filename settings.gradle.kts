import java.util.Properties

pluginManagement {
    repositories {
        mavenLocal()
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
val nexusSnapshotUrl: String = nexusProperties.getProperty("nexus_snapshot_url")

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
        maven {
            url = uri(nexusSnapshotUrl)
        }
        //--------------Nexus Configuration-------------------
    }
}

rootProject.name = "Tap2idSDKAndroidSample"
include(":app")
