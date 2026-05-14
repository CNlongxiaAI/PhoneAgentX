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

rootProject.name = "AgentClaw"
include(":agentclaw-app")

// Gradle Version Catalog compatible settings
gradle {
    wrapperVersion = "8.4"
    wrapperDistributionType = WrapperDistributionType.BIN
}