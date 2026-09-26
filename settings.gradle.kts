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
    }
}

rootProject.name = "KPopDisplay"

include(
    ":app",
    ":core",
    ":feature-scan",
    ":feature-upscale",
    ":feature-display",
    ":feature-admin",
)
