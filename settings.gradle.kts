pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "TeleDrive"
include(":app")
include(":core:data")
include(":core:telegram")
include(":core:crypto")
include(":feature:auth")
include(":feature:backup")
include(":feature:drive")
